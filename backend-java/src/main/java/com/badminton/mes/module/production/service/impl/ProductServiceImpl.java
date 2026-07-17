package com.badminton.mes.module.production.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.service.ProductRouteReferenceQuery;
import com.badminton.mes.module.integration.service.UnitService;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.ProductPageReqVO;
import com.badminton.mes.module.production.controller.vo.ProductRespVO;
import com.badminton.mes.module.production.controller.vo.ProductSaveReqVO;
import com.badminton.mes.module.production.controller.vo.ProductUpdateReqVO;
import com.badminton.mes.module.production.controller.vo.ProductionStatusReqVO;
import com.badminton.mes.module.production.convert.ProductionMasterDataConvert;
import com.badminton.mes.module.production.dal.entity.ProductEntity;
import com.badminton.mes.module.production.dal.repository.BomRepository;
import com.badminton.mes.module.production.dal.repository.ProductRepository;
import com.badminton.mes.module.production.dal.repository.ProductionMasterDataSpecifications;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.enums.BomStatusEnum;
import com.badminton.mes.module.production.enums.ProductTypeEnum;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;
import com.badminton.mes.module.production.service.ProductService;
import com.badminton.mes.module.production.service.support.ProductionPersistenceExceptionTranslator;
import com.badminton.mes.module.wage.service.ProductPieceRateRuleReferenceQuery;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 产品主档服务实现。 */
@Service
public class ProductServiceImpl implements ProductService {

    private static final String PRODUCT_CODE_CONSTRAINT = "uk_active_product_code";
    private final ProductRepository productRepository;
    private final UnitService unitService;
    private final WorkOrderRepository workOrderRepository;
    private final BomRepository bomRepository;
    private final ProductRouteReferenceQuery routeReferenceQuery;
    private final ProductPieceRateRuleReferenceQuery pieceRateRuleReferenceQuery;

    /** 构造器注入。 */
    public ProductServiceImpl(ProductRepository productRepository,
                              UnitService unitService,
                              WorkOrderRepository workOrderRepository,
                              BomRepository bomRepository,
                              ProductRouteReferenceQuery routeReferenceQuery,
                              ProductPieceRateRuleReferenceQuery pieceRateRuleReferenceQuery) {
        this.productRepository = productRepository;
        this.unitService = unitService;
        this.workOrderRepository = workOrderRepository;
        this.bomRepository = bomRepository;
        this.routeReferenceQuery = routeReferenceQuery;
        this.pieceRateRuleReferenceQuery = pieceRateRuleReferenceQuery;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProduct(ProductSaveReqVO reqVO) {
        // 规范化后的编码才参与查重和入库；单位采用加锁校验，避免产品创建时引用到刚被停用的单位。
        normalize(reqVO);
        validateType(reqVO.getProductType());
        validateStatus(reqVO.getStatus());
        validateCode(reqVO.getProductCode(), null);
        requireEnabledUnitForUpdate(reqVO.getUnitId());
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        ProductEntity product = ProductionMasterDataConvert.toProductEntity(reqVO);
        product.setCreateBy(operatorId);
        product.setUpdateBy(operatorId);
        // 立即 flush 让产品编码唯一约束在当前事务中生效，底层异常由 save 转成稳定业务错误码。
        save(product);
        return product.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(Long id, ProductUpdateReqVO reqVO) {
        // 更新使用产品行悲观锁和前端版本号双重保护，引用检查与字段写入均基于最新主档快照。
        normalize(reqVO);
        validateType(reqVO.getProductType());
        validateStatus(reqVO.getStatus());
        ProductEntity product = requireForUpdate(id);
        validateVersion(product, reqVO.getVersion());
        validateCode(reqVO.getProductCode(), id);
        requireEnabledUnitForUpdate(reqVO.getUnitId());
        if (CommonStatusEnum.DISABLED.getStatus().equals(reqVO.getStatus())
                && !CommonStatusEnum.DISABLED.getStatus().equals(product.getStatus())) {
            // 停用前检查活动工单、生效 BOM、有效工艺路线及计件规则，避免后续业务继续使用该产品。
            validateNoActiveReferences(id);
        }
        ProductionMasterDataConvert.copyProduct(reqVO, product);
        product.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        save(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id, Integer version) {
        // 产品采用逻辑删除；任何历史引用都会阻止删除，以保证工单、BOM 和工艺路线仍可追溯。
        ProductEntity product = requireForUpdate(id);
        validateVersion(product, version);
        validateNoAnyReferences(id);
        product.setDeleted(true);
        product.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        save(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProductStatus(Long id, ProductionStatusReqVO reqVO) {
        // 状态操作独立使用锁版本控制；相同状态重复提交直接幂等返回，不重复更新修改人。
        validateStatus(reqVO.getStatus());
        ProductEntity product = requireForUpdate(id);
        validateVersion(product, reqVO.getVersion());
        if (Objects.equals(product.getStatus(), reqVO.getStatus())) {
            return;
        }
        if (CommonStatusEnum.ENABLED.getStatus().equals(reqVO.getStatus())) {
            // 重新启用前重新锁定并确认计量单位可用，保证产品恢复后能够参与业务单据。
            requireEnabledUnitForUpdate(product.getUnitId());
        } else {
            // 停用只限制当前有效引用，不影响已经产生的历史业务数据。
            validateNoActiveReferences(id);
        }
        product.setStatus(reqVO.getStatus());
        product.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductRespVO getProduct(Long id) {
        // 详情只读取未删除产品并转为 VO，避免直接暴露实体中的审计及持久化字段。
        return ProductionMasterDataConvert.toProductRespVO(require(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProductRespVO> getProductPage(ProductPageReqVO reqVO) {
        // 先校验类型筛选并统计总数；空页不再执行列表查询，越界页码收敛到最后一页。
        validateOptionalType(reqVO.getProductType());
        var specification = ProductionMasterDataSpecifications.productPage(reqVO);
        long total = productRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }
        int pageNo = normalizePageNo(reqVO.getPageNo(), reqVO.getPageSize(), total);
        Page<ProductEntity> page = productRepository.findAll(specification,
                PageRequest.of(pageNo - 1, reqVO.getPageSize(),
                        Sort.by(Sort.Direction.ASC, "productCode").and(Sort.by(Sort.Direction.DESC, "id"))));
        List<ProductRespVO> list = page.getContent().stream()
                .map(ProductionMasterDataConvert::toProductRespVO).toList();
        return PageResult.of(list, total, pageNo, reqVO.getPageSize());
    }

    /**
     * 校验当前业务引用均已解除。
     *
     * <p>各引用通过 exists 查询判断，不加载完整业务集合；只有工单、有效 BOM、路线或计件规则均不再使用时才可停用。
     */
    private void validateNoActiveReferences(Long productId) {
        boolean referenced = workOrderRepository.existsByProductIdAndOrderStatusInAndDeletedFalse(
                productId, WorkOrderStatusEnum.activeStatuses())
                || bomRepository.existsByProductIdAndBomStatusAndDeletedFalse(
                        productId, BomStatusEnum.EFFECTIVE.getStatus())
                || routeReferenceQuery.hasEffectiveRoute(productId)
                || pieceRateRuleReferenceQuery.hasEnabledRule(productId);
        if (referenced) {
            throw new ServiceException(ProductionErrorCodeConstants.PRODUCT_ACTIVE_REFERENCE_EXISTS);
        }
    }

    /** 删除前校验不存在任何历史业务引用，防止主档删除后历史单据无法回填产品信息。 */
    private void validateNoAnyReferences(Long productId) {
        boolean referenced = workOrderRepository.existsByProductIdAndDeletedFalse(productId)
                || bomRepository.existsByProductIdAndDeletedFalse(productId)
                || routeReferenceQuery.hasAnyRouteBinding(productId)
                || pieceRateRuleReferenceQuery.hasAnyRule(productId);
        if (referenced) {
            throw new ServiceException(ProductionErrorCodeConstants.PRODUCT_REFERENCE_EXISTS);
        }
    }

    /** 写锁校验单位存在且启用，防止单位状态在产品写入前发生变化。 */
    private void requireEnabledUnitForUpdate(Long unitId) {
        if (!unitService.lockAndCheckEnabled(unitId)) {
            throw new ServiceException(ProductionErrorCodeConstants.UNIT_NOT_AVAILABLE);
        }
    }

    /** 校验产品编码唯一；更新时排除自身，最终仍由数据库唯一索引兜底并发创建。 */
    private void validateCode(String code, Long excludeId) {
        boolean exists = excludeId == null
                ? productRepository.existsByProductCodeAndDeletedFalse(code)
                : productRepository.existsByProductCodeAndIdNotAndDeletedFalse(code, excludeId);
        if (exists) {
            throw new ServiceException(ProductionErrorCodeConstants.PRODUCT_CODE_DUPLICATE);
        }
    }

    /** 校验产品类型，防止未知枚举值进入产品主档。 */
    private void validateType(Integer type) {
        if (!ProductTypeEnum.contains(type)) {
            throw new ServiceException(com.badminton.mes.common.core.GlobalErrorCodeConstants.PARAM_ERROR,
                    "产品类型不合法");
        }
    }

    /** 校验查询中的可选产品类型；未传值表示不过滤类型。 */
    private void validateOptionalType(Integer type) {
        if (type != null) {
            validateType(type);
        }
    }

    /** 校验产品启停状态，只接受系统定义的启用或停用。 */
    private void validateStatus(Integer status) {
        if (!CommonStatusEnum.ENABLED.getStatus().equals(status)
                && !CommonStatusEnum.DISABLED.getStatus().equals(status)) {
            throw new ServiceException(com.badminton.mes.common.core.GlobalErrorCodeConstants.PARAM_ERROR,
                    "产品状态不合法");
        }
    }

    /** 查询未删除产品，供只读详情使用。 */
    private ProductEntity require(Long id) {
        return productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.PRODUCT_MASTER_NOT_EXISTS));
    }

    /** 写锁查询未删除产品，供更新、删除和状态切换建立稳定快照。 */
    private ProductEntity requireForUpdate(Long id) {
        return productRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.PRODUCT_MASTER_NOT_EXISTS));
    }

    /** 校验客户端携带的乐观锁版本，防止旧页面覆盖其他人的修改。 */
    private void validateVersion(ProductEntity product, Integer expectedVersion) {
        if (!Objects.equals(product.getVersion(), expectedVersion)) {
            throw new ServiceException(ProductionErrorCodeConstants.PRODUCT_CONCURRENT_MODIFICATION);
        }
    }

    /** 保存并转换唯一键和乐观锁异常；立即 flush 使约束冲突在业务方法内暴露。 */
    private void save(ProductEntity product) {
        try {
            productRepository.saveAndFlush(product);
        } catch (DataIntegrityViolationException exception) {
            ProductionPersistenceExceptionTranslator.translateUniqueConstraint(
                    exception, PRODUCT_CODE_CONSTRAINT, ProductionErrorCodeConstants.PRODUCT_CODE_DUPLICATE);
        } catch (OptimisticLockingFailureException exception) {
            throw new ServiceException(ProductionErrorCodeConstants.PRODUCT_CONCURRENT_MODIFICATION);
        }
    }

    /** 规范化产品请求，统一编码大小写、名称空格和可选文本字段。 */
    private void normalize(ProductSaveReqVO reqVO) {
        reqVO.setProductCode(reqVO.getProductCode().trim().toUpperCase(Locale.ROOT));
        reqVO.setProductName(reqVO.getProductName().trim());
        reqVO.setSpec(trimToNull(reqVO.getSpec()));
        reqVO.setGrade(trimToNull(reqVO.getGrade()));
    }

    /** 去除字符串首尾空格，并将空白值转换为数据库中的 null。 */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** 规范化页码，避免请求页码超过最后一页而产生无效分页偏移。 */
    private int normalizePageNo(int requested, int pageSize, long total) {
        return Math.min(requested, (int) ((total + pageSize - 1) / pageSize));
    }
}
