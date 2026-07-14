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
        normalize(reqVO);
        validateType(reqVO.getProductType());
        validateStatus(reqVO.getStatus());
        validateCode(reqVO.getProductCode(), null);
        requireEnabledUnitForUpdate(reqVO.getUnitId());
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        ProductEntity product = ProductionMasterDataConvert.toProductEntity(reqVO);
        product.setCreateBy(operatorId);
        product.setUpdateBy(operatorId);
        save(product);
        return product.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(Long id, ProductUpdateReqVO reqVO) {
        normalize(reqVO);
        validateType(reqVO.getProductType());
        validateStatus(reqVO.getStatus());
        ProductEntity product = requireForUpdate(id);
        validateVersion(product, reqVO.getVersion());
        validateCode(reqVO.getProductCode(), id);
        requireEnabledUnitForUpdate(reqVO.getUnitId());
        if (CommonStatusEnum.DISABLED.getStatus().equals(reqVO.getStatus())
                && !CommonStatusEnum.DISABLED.getStatus().equals(product.getStatus())) {
            validateNoActiveReferences(id);
        }
        ProductionMasterDataConvert.copyProduct(reqVO, product);
        product.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        save(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id, Integer version) {
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
        validateStatus(reqVO.getStatus());
        ProductEntity product = requireForUpdate(id);
        validateVersion(product, reqVO.getVersion());
        if (Objects.equals(product.getStatus(), reqVO.getStatus())) {
            return;
        }
        if (CommonStatusEnum.ENABLED.getStatus().equals(reqVO.getStatus())) {
            requireEnabledUnitForUpdate(product.getUnitId());
        } else {
            validateNoActiveReferences(id);
        }
        product.setStatus(reqVO.getStatus());
        product.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductRespVO getProduct(Long id) {
        return ProductionMasterDataConvert.toProductRespVO(require(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProductRespVO> getProductPage(ProductPageReqVO reqVO) {
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

    /** 校验当前业务引用均已解除。 */
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

    /** 校验不存在任何历史业务引用。 */
    private void validateNoAnyReferences(Long productId) {
        boolean referenced = workOrderRepository.existsByProductIdAndDeletedFalse(productId)
                || bomRepository.existsByProductIdAndDeletedFalse(productId)
                || routeReferenceQuery.hasAnyRouteBinding(productId)
                || pieceRateRuleReferenceQuery.hasAnyRule(productId);
        if (referenced) {
            throw new ServiceException(ProductionErrorCodeConstants.PRODUCT_REFERENCE_EXISTS);
        }
    }

    /** 写锁校验单位存在且启用。 */
    private void requireEnabledUnitForUpdate(Long unitId) {
        if (!unitService.lockAndCheckEnabled(unitId)) {
            throw new ServiceException(ProductionErrorCodeConstants.UNIT_NOT_AVAILABLE);
        }
    }

    /** 校验产品编码唯一。 */
    private void validateCode(String code, Long excludeId) {
        boolean exists = excludeId == null
                ? productRepository.existsByProductCodeAndDeletedFalse(code)
                : productRepository.existsByProductCodeAndIdNotAndDeletedFalse(code, excludeId);
        if (exists) {
            throw new ServiceException(ProductionErrorCodeConstants.PRODUCT_CODE_DUPLICATE);
        }
    }

    /** 校验产品类型。 */
    private void validateType(Integer type) {
        if (!ProductTypeEnum.contains(type)) {
            throw new ServiceException(com.badminton.mes.common.core.GlobalErrorCodeConstants.PARAM_ERROR,
                    "产品类型不合法");
        }
    }

    /** 校验可选产品类型。 */
    private void validateOptionalType(Integer type) {
        if (type != null) {
            validateType(type);
        }
    }

    /** 校验产品启停状态。 */
    private void validateStatus(Integer status) {
        if (!CommonStatusEnum.ENABLED.getStatus().equals(status)
                && !CommonStatusEnum.DISABLED.getStatus().equals(status)) {
            throw new ServiceException(com.badminton.mes.common.core.GlobalErrorCodeConstants.PARAM_ERROR,
                    "产品状态不合法");
        }
    }

    /** 查询未删除产品。 */
    private ProductEntity require(Long id) {
        return productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.PRODUCT_MASTER_NOT_EXISTS));
    }

    /** 写锁查询未删除产品。 */
    private ProductEntity requireForUpdate(Long id) {
        return productRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.PRODUCT_MASTER_NOT_EXISTS));
    }

    /** 校验预期版本。 */
    private void validateVersion(ProductEntity product, Integer expectedVersion) {
        if (!Objects.equals(product.getVersion(), expectedVersion)) {
            throw new ServiceException(ProductionErrorCodeConstants.PRODUCT_CONCURRENT_MODIFICATION);
        }
    }

    /** 保存并转换唯一键和乐观锁异常。 */
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

    /** 规范化产品请求。 */
    private void normalize(ProductSaveReqVO reqVO) {
        reqVO.setProductCode(reqVO.getProductCode().trim().toUpperCase(Locale.ROOT));
        reqVO.setProductName(reqVO.getProductName().trim());
        reqVO.setSpec(trimToNull(reqVO.getSpec()));
        reqVO.setGrade(trimToNull(reqVO.getGrade()));
    }

    /** 字符串去空格并将空白值转为 null。 */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** 规范化页码。 */
    private int normalizePageNo(int requested, int pageSize, long total) {
        return Math.min(requested, (int) ((total + pageSize - 1) / pageSize));
    }
}
