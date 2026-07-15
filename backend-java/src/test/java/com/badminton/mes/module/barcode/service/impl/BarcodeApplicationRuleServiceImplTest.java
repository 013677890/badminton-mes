package com.badminton.mes.module.barcode.service.impl;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.barcode.constants.BarcodeErrorCodeConstants;
import com.badminton.mes.module.barcode.controller.vo.BarcodeApplicationRuleOptionReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeApplicationRulePageReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeApplicationRuleRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeApplicationRuleSaveReqVO;
import com.badminton.mes.module.barcode.dal.entity.BarcodeApplyRuleEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeRuleEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeTemplateEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeTypeEntity;
import com.badminton.mes.module.barcode.dal.entity.ProductRefEntity;
import com.badminton.mes.module.barcode.dal.repository.BarcodeApplyRuleRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeRuleRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeTemplateRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeTypeRepository;
import com.badminton.mes.module.barcode.dal.repository.MaterialRefRepository;
import com.badminton.mes.module.barcode.dal.repository.ProductRefRepository;
import com.badminton.mes.module.barcode.enums.BarcodeApplyObjectTypeEnum;
import com.badminton.mes.module.barcode.enums.BarcodeModeEnum;
import com.badminton.mes.module.barcode.enums.BarcodeSourceTypeEnum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link BarcodeApplicationRuleServiceImpl} 单元测试。
 *
 * <p>数据库依赖全部 Mock。覆盖对象匹配、来源规则必填、三档案可用性校验、
 * 启用默认规则唯一性预检与并发兜底。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@ExtendWith(MockitoExtension.class)
class BarcodeApplicationRuleServiceImplTest {

    /** 测试用应用规则 id */
    private static final Long APPLY_RULE_ID = 400L;

    /** 测试用条码类型 id */
    private static final Long TYPE_ID = 100L;

    /** 测试用条码规则 id */
    private static final Long RULE_ID = 200L;

    /** 测试用模板 id */
    private static final Long TEMPLATE_ID = 300L;

    /** 测试用产品 id */
    private static final Long PRODUCT_ID = 10L;

    @Mock
    private BarcodeApplyRuleRepository barcodeApplyRuleRepository;

    @Mock
    private BarcodeTypeRepository barcodeTypeRepository;

    @Mock
    private BarcodeRuleRepository barcodeRuleRepository;

    @Mock
    private BarcodeTemplateRepository barcodeTemplateRepository;

    @Mock
    private BarcodeRepository barcodeRepository;

    @Mock
    private ProductRefRepository productRefRepository;

    @Mock
    private MaterialRefRepository materialRefRepository;

    private BarcodeApplicationRuleServiceImpl applicationRuleService;

    @BeforeEach
    void setUp() {
        applicationRuleService = new BarcodeApplicationRuleServiceImpl(barcodeApplyRuleRepository,
                barcodeTypeRepository, barcodeRuleRepository, barcodeTemplateRepository, barcodeRepository,
                productRefRepository, materialRefRepository);
    }

    @Test
    @DisplayName("创建应用规则：产品对象合法请求默认启用且默认标记兜底为 true")
    void createApplicationRuleSavesEnabledDefaultRule() {
        stubAvailableReferences();
        when(barcodeApplyRuleRepository.countActiveDefault(
                BarcodeApplyObjectTypeEnum.PRODUCT.getType(), PRODUCT_ID, TYPE_ID, null,
                CommonStatusEnum.ENABLED.getStatus())).thenReturn(0L);
        when(barcodeApplyRuleRepository.saveAndFlush(any(BarcodeApplyRuleEntity.class)))
                .thenAnswer(invocation -> {
                    BarcodeApplyRuleEntity entity = invocation.getArgument(0);
                    entity.setId(APPLY_RULE_ID);
                    return entity;
                });

        Long id = applicationRuleService.createBarcodeApplicationRule(buildSaveReqVO());

        assertThat(id).isEqualTo(APPLY_RULE_ID);
        ArgumentCaptor<BarcodeApplyRuleEntity> captor =
                ArgumentCaptor.forClass(BarcodeApplyRuleEntity.class);
        verify(barcodeApplyRuleRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CommonStatusEnum.ENABLED.getStatus());
        assertThat(captor.getValue().getDefaultFlag()).isTrue();
    }

    @Test
    @DisplayName("创建应用规则：产品对象携带物料 id 时报对象不匹配")
    void createApplicationRuleRejectsObjectMismatch() {
        BarcodeApplicationRuleSaveReqVO reqVO = buildSaveReqVO();
        reqVO.setMaterialId(20L);

        assertThatThrownBy(() -> applicationRuleService.createBarcodeApplicationRule(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_OBJECT_MISMATCH));
        verify(barcodeApplyRuleRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("创建应用规则：产品已停用时拒绝")
    void createApplicationRuleRejectsDisabledProduct() {
        ProductRefEntity product = buildEnabledProduct();
        product.setStatus(CommonStatusEnum.DISABLED.getStatus());
        when(productRefRepository.findByIdAndDeletedFalse(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> applicationRuleService.createBarcodeApplicationRule(buildSaveReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.PRODUCT_NOT_AVAILABLE));
    }

    @Test
    @DisplayName("创建应用规则：来源为规则生成但未选规则时拒绝")
    void createApplicationRuleRejectsMissingRule() {
        when(productRefRepository.findByIdAndDeletedFalse(PRODUCT_ID))
                .thenReturn(Optional.of(buildEnabledProduct()));
        BarcodeApplicationRuleSaveReqVO reqVO = buildSaveReqVO();
        reqVO.setRuleId(null);

        assertThatThrownBy(() -> applicationRuleService.createBarcodeApplicationRule(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_RULE_REQUIRED));
    }

    @Test
    @DisplayName("创建应用规则：条码规则适用类型不匹配时拒绝")
    void createApplicationRuleRejectsRuleTypeMismatch() {
        when(productRefRepository.findByIdAndDeletedFalse(PRODUCT_ID))
                .thenReturn(Optional.of(buildEnabledProduct()));
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(buildEnabledType()));
        BarcodeRuleEntity mismatchedRule = buildEnabledRule();
        mismatchedRule.setBarcodeTypeId(999L);
        when(barcodeRuleRepository.findByIdAndDeletedFalse(RULE_ID))
                .thenReturn(Optional.of(mismatchedRule));

        assertThatThrownBy(() -> applicationRuleService.createBarcodeApplicationRule(buildSaveReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_TYPE_MISMATCH));
    }

    @Test
    @DisplayName("创建应用规则：同对象同类型已有启用默认规则时拒绝")
    void createApplicationRuleRejectsDuplicateActiveDefault() {
        stubAvailableReferences();
        when(barcodeApplyRuleRepository.countActiveDefault(
                BarcodeApplyObjectTypeEnum.PRODUCT.getType(), PRODUCT_ID, TYPE_ID, null,
                CommonStatusEnum.ENABLED.getStatus())).thenReturn(1L);

        assertThatThrownBy(() -> applicationRuleService.createBarcodeApplicationRule(buildSaveReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_DEFAULT_DUPLICATE));
        verify(barcodeApplyRuleRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("创建应用规则：并发穿透预检时由 uk_active_default 兜底转业务错误")
    void createApplicationRuleTranslatesUniqueViolation() {
        stubAvailableReferences();
        when(barcodeApplyRuleRepository.countActiveDefault(any(), any(), any(), any(), any()))
                .thenReturn(0L);
        when(barcodeApplyRuleRepository.saveAndFlush(any(BarcodeApplyRuleEntity.class)))
                .thenThrow(new DataIntegrityViolationException("uk_active_default"));

        assertThatThrownBy(() -> applicationRuleService.createBarcodeApplicationRule(buildSaveReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_DEFAULT_DUPLICATE));
    }

    @Test
    @DisplayName("修改应用规则：CAS 未命中(并发删除)时报不存在")
    void updateApplicationRuleRejectsConcurrentDelete() {
        when(barcodeApplyRuleRepository.findByIdAndDeletedFalse(APPLY_RULE_ID))
                .thenReturn(Optional.of(buildApplyRuleEntity()));
        stubAvailableReferences();
        when(barcodeApplyRuleRepository.countActiveDefault(any(), any(), any(), any(), any()))
                .thenReturn(0L);
        when(barcodeApplyRuleRepository.updateInfo(any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> applicationRuleService.updateBarcodeApplicationRule(APPLY_RULE_ID,
                buildSaveReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_NOT_EXISTS));
    }

    @Test
    @DisplayName("启用应用规则：引用的模板已停用时拒绝启用")
    void enableApplicationRuleRejectsDisabledTemplate() {
        when(barcodeApplyRuleRepository.findByIdAndDeletedFalse(APPLY_RULE_ID))
                .thenReturn(Optional.of(buildApplyRuleEntity()));
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(buildEnabledType()));
        when(barcodeRuleRepository.findByIdAndDeletedFalse(RULE_ID))
                .thenReturn(Optional.of(buildEnabledRule()));
        BarcodeTemplateEntity disabledTemplate = buildEnabledTemplate();
        disabledTemplate.setStatus(CommonStatusEnum.DISABLED.getStatus());
        when(barcodeTemplateRepository.findByIdAndDeletedFalse(TEMPLATE_ID))
                .thenReturn(Optional.of(disabledTemplate));

        assertThatThrownBy(() -> applicationRuleService.enableBarcodeApplicationRule(APPLY_RULE_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_TEMPLATE_NOT_AVAILABLE));
        verify(barcodeApplyRuleRepository, never()).updateStatus(any(), any(), any());
    }

    @Test
    @DisplayName("启用应用规则：默认规则唯一性预检不通过时拒绝")
    void enableApplicationRuleRejectsDuplicateActiveDefault() {
        when(barcodeApplyRuleRepository.findByIdAndDeletedFalse(APPLY_RULE_ID))
                .thenReturn(Optional.of(buildApplyRuleEntity()));
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(buildEnabledType()));
        when(barcodeRuleRepository.findByIdAndDeletedFalse(RULE_ID))
                .thenReturn(Optional.of(buildEnabledRule()));
        when(barcodeTemplateRepository.findByIdAndDeletedFalse(TEMPLATE_ID))
                .thenReturn(Optional.of(buildEnabledTemplate()));
        when(barcodeApplyRuleRepository.countActiveDefault(
                BarcodeApplyObjectTypeEnum.PRODUCT.getType(), PRODUCT_ID, TYPE_ID, APPLY_RULE_ID,
                CommonStatusEnum.ENABLED.getStatus())).thenReturn(1L);

        assertThatThrownBy(() -> applicationRuleService.enableBarcodeApplicationRule(APPLY_RULE_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_DEFAULT_DUPLICATE));
    }

    @Test
    @DisplayName("停用应用规则：已停用时报状态错误")
    void disableApplicationRuleRejectsAlreadyDisabled() {
        when(barcodeApplyRuleRepository.updateStatus(APPLY_RULE_ID,
                CommonStatusEnum.ENABLED.getStatus(), CommonStatusEnum.DISABLED.getStatus()))
                .thenReturn(0);
        when(barcodeApplyRuleRepository.findByIdAndDeletedFalse(APPLY_RULE_ID))
                .thenReturn(Optional.of(buildApplyRuleEntity()));

        assertThatThrownBy(() -> applicationRuleService.disableBarcodeApplicationRule(APPLY_RULE_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_ALREADY_DISABLED));
    }

    @Test
    @DisplayName("删除应用规则：已生成条码时拒绝删除")
    void deleteApplicationRuleRejectsGeneratedBarcodes() {
        when(barcodeApplyRuleRepository.findByIdAndDeletedFalse(APPLY_RULE_ID))
                .thenReturn(Optional.of(buildApplyRuleEntity()));
        when(barcodeRepository.existsByApplyRuleIdAndDeletedFalse(APPLY_RULE_ID)).thenReturn(true);

        assertThatThrownBy(() -> applicationRuleService.deleteBarcodeApplicationRule(APPLY_RULE_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_IN_USE_NOT_DELETE));
        verify(barcodeApplyRuleRepository, never()).logicDeleteById(any());
    }

    @Test
    @DisplayName("删除应用规则：未使用时逻辑删除")
    void deleteApplicationRuleLogicallyDeletes() {
        when(barcodeApplyRuleRepository.findByIdAndDeletedFalse(APPLY_RULE_ID))
                .thenReturn(Optional.of(buildApplyRuleEntity()));
        when(barcodeRepository.existsByApplyRuleIdAndDeletedFalse(APPLY_RULE_ID)).thenReturn(false);
        when(barcodeApplyRuleRepository.logicDeleteById(APPLY_RULE_ID)).thenReturn(1);

        applicationRuleService.deleteBarcodeApplicationRule(APPLY_RULE_ID);

        verify(barcodeApplyRuleRepository).logicDeleteById(APPLY_RULE_ID);
    }

    @Test
    @DisplayName("选项查询：按对象过滤转发启用状态查询")
    void getApplicationRuleOptionsForwardsEnabledQuery() {
        when(barcodeApplyRuleRepository.findEnabledOptions(
                BarcodeApplyObjectTypeEnum.PRODUCT.getType(), PRODUCT_ID, null, TYPE_ID,
                CommonStatusEnum.ENABLED.getStatus())).thenReturn(List.of(buildApplyRuleEntity()));

        BarcodeApplicationRuleOptionReqVO reqVO = new BarcodeApplicationRuleOptionReqVO();
        reqVO.setObjectType(BarcodeApplyObjectTypeEnum.PRODUCT.getType());
        reqVO.setProductId(PRODUCT_ID);
        reqVO.setBarcodeTypeId(TYPE_ID);

        List<BarcodeApplicationRuleRespVO> options =
                applicationRuleService.getBarcodeApplicationRuleOptions(reqVO);

        assertThat(options).hasSize(1);
        assertThat(options.get(0).getId()).isEqualTo(APPLY_RULE_ID);
    }

    @Test
    @DisplayName("分页查询：总数为 0 时直接返回空页(SQL-005)")
    void getApplicationRulePageShortCircuitsOnZeroTotal() {
        when(barcodeApplyRuleRepository.count(any(Specification.class))).thenReturn(0L);

        PageResult<BarcodeApplicationRuleRespVO> result =
                applicationRuleService.getBarcodeApplicationRulePage(new BarcodeApplicationRulePageReqVO());

        assertThat(result.getTotal()).isZero();
        assertThat(result.getList()).isEmpty();
    }

    /**
     * Mock 产品、类型、规则、模板均可用。
     */
    private void stubAvailableReferences() {
        when(productRefRepository.findByIdAndDeletedFalse(PRODUCT_ID))
                .thenReturn(Optional.of(buildEnabledProduct()));
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(buildEnabledType()));
        when(barcodeRuleRepository.findByIdAndDeletedFalse(RULE_ID))
                .thenReturn(Optional.of(buildEnabledRule()));
        when(barcodeTemplateRepository.findByIdAndDeletedFalse(TEMPLATE_ID))
                .thenReturn(Optional.of(buildEnabledTemplate()));
    }

    /**
     * 构造合法保存请求：产品对象 + 规则生成来源。
     *
     * @return 保存请求 VO
     */
    private BarcodeApplicationRuleSaveReqVO buildSaveReqVO() {
        BarcodeApplicationRuleSaveReqVO reqVO = new BarcodeApplicationRuleSaveReqVO();
        reqVO.setObjectType(BarcodeApplyObjectTypeEnum.PRODUCT.getType());
        reqVO.setProductId(PRODUCT_ID);
        reqVO.setBarcodeTypeId(TYPE_ID);
        reqVO.setBarcodeMode(BarcodeModeEnum.BATCH.getMode());
        reqVO.setRuleId(RULE_ID);
        reqVO.setTemplateId(TEMPLATE_ID);
        reqVO.setSourceType(BarcodeSourceTypeEnum.RULE_GENERATE.getType());
        return reqVO;
    }

    /**
     * 构造启用状态的应用规则实体。
     *
     * @return 应用规则实体
     */
    private BarcodeApplyRuleEntity buildApplyRuleEntity() {
        BarcodeApplyRuleEntity entity = new BarcodeApplyRuleEntity();
        entity.setId(APPLY_RULE_ID);
        entity.setObjectType(BarcodeApplyObjectTypeEnum.PRODUCT.getType());
        entity.setProductId(PRODUCT_ID);
        entity.setBarcodeTypeId(TYPE_ID);
        entity.setBarcodeMode(BarcodeModeEnum.BATCH.getMode());
        entity.setRuleId(RULE_ID);
        entity.setTemplateId(TEMPLATE_ID);
        entity.setSourceType(BarcodeSourceTypeEnum.RULE_GENERATE.getType());
        entity.setDefaultFlag(true);
        entity.setStatus(CommonStatusEnum.ENABLED.getStatus());
        entity.setDeleted(false);
        return entity;
    }

    /**
     * 构造启用状态的产品只读引用。
     *
     * @return 产品只读引用
     */
    private ProductRefEntity buildEnabledProduct() {
        ProductRefEntity product = new ProductRefEntity();
        product.setId(PRODUCT_ID);
        product.setProductCode("YMQ01");
        product.setStatus(CommonStatusEnum.ENABLED.getStatus());
        product.setDeleted(false);
        return product;
    }

    /**
     * 构造启用状态的条码类型。
     *
     * @return 类型实体
     */
    private BarcodeTypeEntity buildEnabledType() {
        BarcodeTypeEntity entity = new BarcodeTypeEntity();
        entity.setId(TYPE_ID);
        entity.setStatus(CommonStatusEnum.ENABLED.getStatus());
        entity.setDeleted(false);
        return entity;
    }

    /**
     * 构造启用状态且适用于测试类型的条码规则。
     *
     * @return 规则实体
     */
    private BarcodeRuleEntity buildEnabledRule() {
        BarcodeRuleEntity entity = new BarcodeRuleEntity();
        entity.setId(RULE_ID);
        entity.setBarcodeTypeId(TYPE_ID);
        entity.setStatus(CommonStatusEnum.ENABLED.getStatus());
        entity.setDeleted(false);
        return entity;
    }

    /**
     * 构造启用状态的条码模板。
     *
     * @return 模板实体
     */
    private BarcodeTemplateEntity buildEnabledTemplate() {
        BarcodeTemplateEntity entity = new BarcodeTemplateEntity();
        entity.setId(TEMPLATE_ID);
        entity.setStatus(CommonStatusEnum.ENABLED.getStatus());
        entity.setDeleted(false);
        return entity;
    }
}
