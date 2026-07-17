package com.badminton.mes.module.production.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.BomActionReqVO;
import com.badminton.mes.module.production.controller.vo.BomNewVersionReqVO;
import com.badminton.mes.module.production.controller.vo.BomPageReqVO;
import com.badminton.mes.module.production.controller.vo.BomRespVO;
import com.badminton.mes.module.production.controller.vo.BomSaveReqVO;
import com.badminton.mes.module.production.controller.vo.BomUpdateReqVO;
import com.badminton.mes.module.production.convert.ProductionMasterDataConvert;
import com.badminton.mes.module.production.dal.entity.BomDetailEntity;
import com.badminton.mes.module.production.dal.entity.BomEntity;
import com.badminton.mes.module.production.dal.entity.ProductEntity;
import com.badminton.mes.module.production.dal.repository.BomRepository;
import com.badminton.mes.module.production.dal.repository.ProductRepository;
import com.badminton.mes.module.production.dal.repository.ProductionMasterDataSpecifications;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.enums.BomStatusEnum;
import com.badminton.mes.module.production.service.BomService;
import com.badminton.mes.module.production.service.support.BomDetailManager;
import com.badminton.mes.module.production.service.support.ProductionPersistenceExceptionTranslator;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** BOM 版本与状态服务实现。 */
@Service
public class BomServiceImpl implements BomService {

    private static final String BOM_CODE_CONSTRAINT = "uk_active_bom_code";
    private static final String PRODUCT_VERSION_CONSTRAINT = "uk_active_product_version";

    private final BomRepository bomRepository;
    private final ProductRepository productRepository;
    private final WorkOrderRepository workOrderRepository;
    private final BomDetailManager detailManager;

    /** 构造器注入。 */
    public BomServiceImpl(BomRepository bomRepository,
                          ProductRepository productRepository,
                          WorkOrderRepository workOrderRepository,
                          BomDetailManager detailManager) {
        this.bomRepository = bomRepository;
        this.productRepository = productRepository;
        this.workOrderRepository = workOrderRepository;
        this.detailManager = detailManager;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBom(BomSaveReqVO reqVO) {
        // 先统一编码和版本的大小写，再锁定产品及明细引用的物料；这样同一事务中的唯一性判断
        // 使用的就是最终入库值，并且物料停用与 BOM 创建不能在校验后悄悄穿插。
        normalize(reqVO);
        requireEnabledProductForUpdate(reqVO.getProductId());
        detailManager.validateAndLock(reqVO.getDetails());
        // 应用层查重只负责提供明确的业务提示，最终仍由数据库唯一索引承担并发场景下的兜底。
        validateUnique(reqVO.getBomCode(), reqVO.getProductId(), reqVO.getVersion(), null);
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        BomEntity bom = newBom(reqVO.getBomCode(), reqVO.getProductId(), reqVO.getVersion(), operatorId);
        // 先保存主表拿到 BOM 主键，再写明细表外键；明细写入失败会随事务一起回滚，避免产生空壳 BOM。
        save(bom);
        detailManager.insert(bom.getId(), reqVO.getDetails());
        return bom.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBom(Long id, BomUpdateReqVO reqVO) {
        // 更新仍沿用创建时的规范化、产品锁和明细锁，保证修改后的主表引用与明细快照在一个事务内一致。
        normalize(reqVO);
        requireEnabledProductForUpdate(reqVO.getProductId());
        BomEntity bom = requireForUpdate(id);
        // 乐观锁版本由前端回传，防止用户基于旧页面覆盖其他人刚提交的 BOM 修改。
        validateLockVersion(bom, reqVO.getLockVersion());
        requireDraft(bom);
        detailManager.validateAndLock(reqVO.getDetails());
        validateUnique(reqVO.getBomCode(), reqVO.getProductId(), reqVO.getVersion(), id);
        bom.setBomCode(reqVO.getBomCode());
        bom.setProductId(reqVO.getProductId());
        bom.setVersion(reqVO.getVersion());
        bom.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        // 主表与明细均使用当前事务提交；任意一个数据库操作失败，调用方看到的 BOM 仍保持原子状态。
        save(bom);
        detailManager.replace(id, reqVO.getDetails());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBom(Long id, Integer lockVersion) {
        // 删除前锁定并校验草稿状态；已被工单引用的 BOM 必须保留，避免历史工单失去可追溯版本。
        BomEntity bom = requireForUpdate(id);
        validateLockVersion(bom, lockVersion);
        requireDraft(bom);
        if (workOrderRepository.existsByBomIdAndDeletedFalse(id)) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_REFERENCED_BY_WORK_ORDER);
        }
        // 先删除明细再删除主表，两个动作都是逻辑删除，保证任何历史查询仍可追踪原版本。
        detailManager.softDelete(id);
        bom.setDeleted(true);
        bom.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        save(bom);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateBom(Long id, BomActionReqVO reqVO) {
        // 产品行作为同一产品 BOM 的总锁；锁住该产品下所有 BOM 后，才能安全地执行“只能有一个生效版本”。
        BomEntity snapshot = require(id);
        ProductEntity product = requireEnabledProductForUpdate(snapshot.getProductId());
        List<BomEntity> productBoms = bomRepository
                .findByProductIdForUpdateOrderByIdAsc(product.getId());
        BomEntity target = findLockedTarget(productBoms, id);
        validateLockVersion(target, reqVO.getLockVersion());
        if (!BomStatusEnum.DRAFT.getStatus().equals(target.getBomStatus())
                && !BomStatusEnum.DISABLED.getStatus().equals(target.getBomStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_STATUS_INVALID);
        }
        List<BomDetailEntity> details = detailManager.getActiveDetails(id);
        // 生效前再次锁定现有明细，防止物料在校验通过后被停用或删除。
        detailManager.validateAndLockExisting(details);
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        productBoms.stream()
                .filter(bom -> !bom.getId().equals(id))
                .filter(bom -> BomStatusEnum.EFFECTIVE.getStatus().equals(bom.getBomStatus()))
                .forEach(bom -> {
                    // 同产品已生效版本降为停用，再把目标版本切换为生效，避免出现两个有效版本。
                    bom.setBomStatus(BomStatusEnum.DISABLED.getStatus());
                    bom.setUpdateBy(operatorId);
                });
        target.setBomStatus(BomStatusEnum.EFFECTIVE.getStatus());
        target.setUpdateBy(operatorId);
        saveAll(productBoms);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableBom(Long id, BomActionReqVO reqVO) {
        // 停用只允许针对当前生效版本执行，并通过锁版本拒绝旧页面的重复操作。
        BomEntity bom = requireForUpdate(id);
        validateLockVersion(bom, reqVO.getLockVersion());
        if (!BomStatusEnum.EFFECTIVE.getStatus().equals(bom.getBomStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_STATUS_INVALID);
        }
        bom.setBomStatus(BomStatusEnum.DISABLED.getStatus());
        bom.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        save(bom);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createNewVersion(Long id, BomNewVersionReqVO reqVO) {
        // 新版本复制的是已锁定的源版本快照；源版本不能是草稿，避免把未完成编辑内容发布成正式版本。
        normalize(reqVO);
        BomEntity snapshot = require(id);
        requireEnabledProductForUpdate(snapshot.getProductId());
        List<BomEntity> productBoms = bomRepository
                .findByProductIdForUpdateOrderByIdAsc(snapshot.getProductId());
        BomEntity source = findLockedTarget(productBoms, id);
        validateLockVersion(source, reqVO.getLockVersion());
        if (BomStatusEnum.DRAFT.getStatus().equals(source.getBomStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_STATUS_INVALID);
        }
        List<BomDetailEntity> sourceDetails = detailManager.getActiveDetails(id);
        detailManager.validateAndLockExisting(sourceDetails);
        validateUnique(reqVO.getBomCode(), source.getProductId(), reqVO.getVersion(), null);
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        BomEntity target = newBom(reqVO.getBomCode(), source.getProductId(), reqVO.getVersion(), operatorId);
        // 主表落库后复制明细，复制失败会回滚新版本主表，不留下缺明细的半成品版本。
        save(target);
        detailManager.copyTo(target.getId(), sourceDetails);
        return target.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public BomRespVO getBom(Long id) {
        // 详情查询只读主表、产品和有效明细；产品名称及物料信息由转换层统一组装，避免返回内部实体。
        BomEntity bom = require(id);
        ProductEntity product = productRepository.findById(bom.getProductId()).orElse(null);
        List<BomDetailEntity> details = detailManager.getActiveDetails(id);
        return ProductionMasterDataConvert.toBomRespVO(
                bom, product, details, detailManager.getMaterialMap(details));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BomRespVO> getBomPage(BomPageReqVO reqVO) {
        // 分页先 count 再查当前页，空结果直接返回，避免无数据时再次访问主表列表。
        validateOptionalStatus(reqVO.getBomStatus());
        var specification = ProductionMasterDataSpecifications.bomPage(reqVO);
        long total = bomRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }
        int pageNo = normalizePageNo(reqVO.getPageNo(), reqVO.getPageSize(), total);
        Page<BomEntity> page = bomRepository.findAll(specification,
                PageRequest.of(pageNo - 1, reqVO.getPageSize(),
                        Sort.by(Sort.Direction.ASC, "bomCode")
                                .and(Sort.by(Sort.Direction.DESC, "id"))));
        Map<Long, ProductEntity> products = productRepository.findAllById(page.getContent().stream()
                        .map(BomEntity::getProductId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));
        // 产品只按当前页涉及的主键批量查询，避免逐条加载产品造成 N+1 数据库访问。
        List<BomRespVO> list = page.getContent().stream()
                .map(bom -> ProductionMasterDataConvert.toBomSummaryRespVO(
                        bom, products.get(bom.getProductId())))
                .toList();
        return PageResult.of(list, total, pageNo, reqVO.getPageSize());
    }

    /**
     * 写锁校验产品存在且启用。
     *
     * <p>这里的数据库行锁与 BOM 状态切换共用同一把产品锁，保证产品停用不会与 BOM 生效并发穿插。
     */
    private ProductEntity requireEnabledProductForUpdate(Long productId) {
        ProductEntity product = productRepository.findByIdAndDeletedFalseForUpdate(productId)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.PRODUCT_NOT_EXISTS));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(product.getStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.PRODUCT_NOT_EXISTS);
        }
        return product;
    }

    /** 查询未删除 BOM，详情读取不需要锁，写操作会使用对应的加锁版本。 */
    private BomEntity require(Long id) {
        return bomRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.BOM_NOT_EXISTS));
    }

    /** 写锁查询未删除 BOM，供更新、删除和状态变更建立最新数据库快照。 */
    private BomEntity requireForUpdate(Long id) {
        return bomRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.BOM_NOT_EXISTS));
    }

    /**
     * 从已锁定产品 BOM 集合定位目标，集合变化视为并发冲突。
     *
     * <p>不重新查询未加锁实体，避免后续操作基于与产品锁不一致的快照继续写库。
     */
    private BomEntity findLockedTarget(List<BomEntity> boms, Long id) {
        return boms.stream().filter(bom -> bom.getId().equals(id)).findFirst()
                .orElseThrow(() -> new ServiceException(
                        ProductionErrorCodeConstants.BOM_CONCURRENT_MODIFICATION));
    }

    /** 校验客户端预期锁版本，阻止旧页面覆盖最新提交。 */
    private void validateLockVersion(BomEntity bom, Integer expectedVersion) {
        if (!Objects.equals(bom.getLockVersion(), expectedVersion)) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_CONCURRENT_MODIFICATION);
        }
    }

    /** 校验 BOM 为草稿；只有草稿允许编辑或删除主表与明细。 */
    private void requireDraft(BomEntity bom) {
        if (!BomStatusEnum.DRAFT.getStatus().equals(bom.getBomStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_NOT_DRAFT);
        }
    }

    /**
     * 校验 BOM 编码及产品业务版本唯一。
     *
     * <p>更新时排除自身主键；该查询是友好提示，提交阶段仍需依赖唯一索引处理并发竞争。
     */
    private void validateUnique(String code, Long productId, String version, Long excludeId) {
        boolean codeExists = excludeId == null
                ? bomRepository.existsByBomCodeAndDeletedFalse(code)
                : bomRepository.existsByBomCodeAndIdNotAndDeletedFalse(code, excludeId);
        if (codeExists) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_CODE_DUPLICATE);
        }
        boolean versionExists = excludeId == null
                ? bomRepository.existsByProductIdAndVersionAndDeletedFalse(productId, version)
                : bomRepository.existsByProductIdAndVersionAndIdNotAndDeletedFalse(
                        productId, version, excludeId);
        if (versionExists) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_PRODUCT_VERSION_DUPLICATE);
        }
    }

    /**
     * 保存 BOM 并转换数据库约束与乐观锁异常。
     *
     * <p>立即 flush 是为了在当前事务中尽早发现唯一键和版本冲突，并把基础设施异常映射为业务错误码。
     */
    private void save(BomEntity bom) {
        try {
            bomRepository.saveAndFlush(bom);
        } catch (DataIntegrityViolationException exception) {
            translateConstraint(exception);
        } catch (OptimisticLockingFailureException exception) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_CONCURRENT_MODIFICATION);
        }
    }

    /** 批量保存同一产品下的状态切换，乐观锁冲突统一转为并发修改提示。 */
    private void saveAll(List<BomEntity> boms) {
        try {
            bomRepository.saveAllAndFlush(boms);
        } catch (OptimisticLockingFailureException exception) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_CONCURRENT_MODIFICATION);
        }
    }

    /** 精确转换 BOM 唯一约束；未知约束继续抛出，避免掩盖其他数据完整性问题。 */
    private void translateConstraint(DataIntegrityViolationException exception) {
        if (ProductionPersistenceExceptionTranslator.isConstraintViolation(
                exception, BOM_CODE_CONSTRAINT)) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_CODE_DUPLICATE);
        }
        if (ProductionPersistenceExceptionTranslator.isConstraintViolation(
                exception, PRODUCT_VERSION_CONSTRAINT)) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_PRODUCT_VERSION_DUPLICATE);
        }
        throw exception;
    }

    /** 构造只包含主表版本、状态及审计字段的 BOM 草稿实体。 */
    private BomEntity newBom(String code, Long productId, String version, Long operatorId) {
        BomEntity bom = new BomEntity();
        bom.setBomCode(code);
        bom.setProductId(productId);
        bom.setVersion(version);
        bom.setBomStatus(BomStatusEnum.DRAFT.getStatus());
        bom.setCreateBy(operatorId);
        bom.setUpdateBy(operatorId);
        return bom;
    }

    /** 规范化 BOM 保存请求，统一去除首尾空格并转为大写后再参与查重和入库。 */
    private void normalize(BomSaveReqVO reqVO) {
        reqVO.setBomCode(reqVO.getBomCode().trim().toUpperCase(Locale.ROOT));
        reqVO.setVersion(reqVO.getVersion().trim().toUpperCase(Locale.ROOT));
    }

    /** 规范化新版本请求，使复制版本与普通创建遵循相同的编码规则。 */
    private void normalize(BomNewVersionReqVO reqVO) {
        reqVO.setBomCode(reqVO.getBomCode().trim().toUpperCase(Locale.ROOT));
        reqVO.setVersion(reqVO.getVersion().trim().toUpperCase(Locale.ROOT));
    }

    /** 校验可选 BOM 状态，避免把未知状态值拼接进动态查询条件。 */
    private void validateOptionalStatus(Integer status) {
        if (status != null && !BomStatusEnum.contains(status)) {
            throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR, "BOM 状态不合法");
        }
    }

    /** 将超过最后一页的请求页码收敛到最后一页，保持分页接口可返回有效数据。 */
    private int normalizePageNo(int requested, int pageSize, long total) {
        return Math.min(requested, (int) ((total + pageSize - 1) / pageSize));
    }
}
