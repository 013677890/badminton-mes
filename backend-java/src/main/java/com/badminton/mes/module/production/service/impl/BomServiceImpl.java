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
        normalize(reqVO);
        requireEnabledProductForUpdate(reqVO.getProductId());
        detailManager.validateAndLock(reqVO.getDetails());
        validateUnique(reqVO.getBomCode(), reqVO.getProductId(), reqVO.getVersion(), null);
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        BomEntity bom = newBom(reqVO.getBomCode(), reqVO.getProductId(), reqVO.getVersion(), operatorId);
        save(bom);
        detailManager.insert(bom.getId(), reqVO.getDetails());
        return bom.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBom(Long id, BomUpdateReqVO reqVO) {
        normalize(reqVO);
        requireEnabledProductForUpdate(reqVO.getProductId());
        BomEntity bom = requireForUpdate(id);
        validateLockVersion(bom, reqVO.getLockVersion());
        requireDraft(bom);
        detailManager.validateAndLock(reqVO.getDetails());
        validateUnique(reqVO.getBomCode(), reqVO.getProductId(), reqVO.getVersion(), id);
        bom.setBomCode(reqVO.getBomCode());
        bom.setProductId(reqVO.getProductId());
        bom.setVersion(reqVO.getVersion());
        bom.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        save(bom);
        detailManager.replace(id, reqVO.getDetails());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBom(Long id, Integer lockVersion) {
        BomEntity bom = requireForUpdate(id);
        validateLockVersion(bom, lockVersion);
        requireDraft(bom);
        if (workOrderRepository.existsByBomIdAndDeletedFalse(id)) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_REFERENCED_BY_WORK_ORDER);
        }
        detailManager.softDelete(id);
        bom.setDeleted(true);
        bom.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        save(bom);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateBom(Long id, BomActionReqVO reqVO) {
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
        detailManager.validateAndLockExisting(details);
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        productBoms.stream()
                .filter(bom -> !bom.getId().equals(id))
                .filter(bom -> BomStatusEnum.EFFECTIVE.getStatus().equals(bom.getBomStatus()))
                .forEach(bom -> {
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
        save(target);
        detailManager.copyTo(target.getId(), sourceDetails);
        return target.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public BomRespVO getBom(Long id) {
        BomEntity bom = require(id);
        ProductEntity product = productRepository.findById(bom.getProductId()).orElse(null);
        List<BomDetailEntity> details = detailManager.getActiveDetails(id);
        return ProductionMasterDataConvert.toBomRespVO(
                bom, product, details, detailManager.getMaterialMap(details));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BomRespVO> getBomPage(BomPageReqVO reqVO) {
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
        List<BomRespVO> list = page.getContent().stream()
                .map(bom -> ProductionMasterDataConvert.toBomSummaryRespVO(
                        bom, products.get(bom.getProductId())))
                .toList();
        return PageResult.of(list, total, pageNo, reqVO.getPageSize());
    }

    /** 写锁校验产品存在且启用。 */
    private ProductEntity requireEnabledProductForUpdate(Long productId) {
        ProductEntity product = productRepository.findByIdAndDeletedFalseForUpdate(productId)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.PRODUCT_NOT_EXISTS));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(product.getStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.PRODUCT_NOT_EXISTS);
        }
        return product;
    }

    /** 查询未删除 BOM。 */
    private BomEntity require(Long id) {
        return bomRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.BOM_NOT_EXISTS));
    }

    /** 写锁查询未删除 BOM。 */
    private BomEntity requireForUpdate(Long id) {
        return bomRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.BOM_NOT_EXISTS));
    }

    /** 从已锁定产品 BOM 集合定位目标，集合变化视为并发冲突。 */
    private BomEntity findLockedTarget(List<BomEntity> boms, Long id) {
        return boms.stream().filter(bom -> bom.getId().equals(id)).findFirst()
                .orElseThrow(() -> new ServiceException(
                        ProductionErrorCodeConstants.BOM_CONCURRENT_MODIFICATION));
    }

    /** 校验客户端预期锁版本。 */
    private void validateLockVersion(BomEntity bom, Integer expectedVersion) {
        if (!Objects.equals(bom.getLockVersion(), expectedVersion)) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_CONCURRENT_MODIFICATION);
        }
    }

    /** 校验 BOM 为草稿。 */
    private void requireDraft(BomEntity bom) {
        if (!BomStatusEnum.DRAFT.getStatus().equals(bom.getBomStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_NOT_DRAFT);
        }
    }

    /** 校验 BOM 编码及产品业务版本唯一。 */
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

    /** 保存 BOM 并转换数据库约束与乐观锁异常。 */
    private void save(BomEntity bom) {
        try {
            bomRepository.saveAndFlush(bom);
        } catch (DataIntegrityViolationException exception) {
            translateConstraint(exception);
        } catch (OptimisticLockingFailureException exception) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_CONCURRENT_MODIFICATION);
        }
    }

    /** 批量保存 BOM 状态切换。 */
    private void saveAll(List<BomEntity> boms) {
        try {
            bomRepository.saveAllAndFlush(boms);
        } catch (OptimisticLockingFailureException exception) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_CONCURRENT_MODIFICATION);
        }
    }

    /** 精确转换 BOM 唯一约束。 */
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

    /** 构造 BOM 草稿。 */
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

    /** 规范化 BOM 保存请求。 */
    private void normalize(BomSaveReqVO reqVO) {
        reqVO.setBomCode(reqVO.getBomCode().trim().toUpperCase(Locale.ROOT));
        reqVO.setVersion(reqVO.getVersion().trim().toUpperCase(Locale.ROOT));
    }

    /** 规范化新版本请求。 */
    private void normalize(BomNewVersionReqVO reqVO) {
        reqVO.setBomCode(reqVO.getBomCode().trim().toUpperCase(Locale.ROOT));
        reqVO.setVersion(reqVO.getVersion().trim().toUpperCase(Locale.ROOT));
    }

    /** 校验可选 BOM 状态。 */
    private void validateOptionalStatus(Integer status) {
        if (status != null && !BomStatusEnum.contains(status)) {
            throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR, "BOM 状态不合法");
        }
    }

    /** 规范化越界页码。 */
    private int normalizePageNo(int requested, int pageSize, long total) {
        return Math.min(requested, (int) ((total + pageSize - 1) / pageSize));
    }
}
