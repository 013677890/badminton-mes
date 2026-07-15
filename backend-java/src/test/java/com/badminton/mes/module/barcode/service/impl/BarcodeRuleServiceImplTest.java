package com.badminton.mes.module.barcode.service.impl;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.barcode.constants.BarcodeErrorCodeConstants;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleItemSaveReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRulePageReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRulePreviewReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRulePreviewRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleSaveReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleValidateReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleValidateRespVO;
import com.badminton.mes.module.barcode.dal.entity.BarcodeRuleEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeRuleItemEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeTypeEntity;
import com.badminton.mes.module.barcode.dal.repository.BarcodeApplyRuleRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeRuleItemRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeRuleRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeSerialRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeTypeRepository;
import com.badminton.mes.module.barcode.enums.BarcodeRuleItemTypeEnum;
import com.badminton.mes.module.barcode.enums.BarcodeSerialResetCycleEnum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link BarcodeRuleServiceImpl} 单元测试。
 *
 * <p>数据库依赖全部 Mock。覆盖规则与明细同事务保存、配置校验拦截、
 * 引用占用删除保护、预览与校验计算路径。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@ExtendWith(MockitoExtension.class)
class BarcodeRuleServiceImplTest {

    /** 测试用规则 id */
    private static final Long RULE_ID = 200L;

    /** 测试用条码类型 id */
    private static final Long TYPE_ID = 100L;

    @Mock
    private BarcodeRuleRepository barcodeRuleRepository;

    @Mock
    private BarcodeRuleItemRepository barcodeRuleItemRepository;

    @Mock
    private BarcodeTypeRepository barcodeTypeRepository;

    @Mock
    private BarcodeApplyRuleRepository barcodeApplyRuleRepository;

    @Mock
    private BarcodeSerialRepository barcodeSerialRepository;

    private BarcodeRuleServiceImpl barcodeRuleService;

    @BeforeEach
    void setUp() {
        barcodeRuleService = new BarcodeRuleServiceImpl(barcodeRuleRepository, barcodeRuleItemRepository,
                barcodeTypeRepository, barcodeApplyRuleRepository, barcodeSerialRepository);
    }

    @Test
    @DisplayName("创建规则：类型可用且配置合法时保存规则与明细并默认启用")
    void createBarcodeRuleSavesRuleAndItems() {
        BarcodeRuleSaveReqVO reqVO = buildSaveReqVO();
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(buildEnabledType()));
        when(barcodeRuleRepository.existsByRuleCodeAndDeletedFalse("RULE01")).thenReturn(false);
        when(barcodeRuleRepository.saveAndFlush(any(BarcodeRuleEntity.class))).thenAnswer(invocation -> {
            BarcodeRuleEntity entity = invocation.getArgument(0);
            entity.setId(RULE_ID);
            return entity;
        });

        Long id = barcodeRuleService.createBarcodeRule(reqVO);

        assertThat(id).isEqualTo(RULE_ID);
        ArgumentCaptor<BarcodeRuleEntity> ruleCaptor = ArgumentCaptor.forClass(BarcodeRuleEntity.class);
        verify(barcodeRuleRepository).saveAndFlush(ruleCaptor.capture());
        assertThat(ruleCaptor.getValue().getStatus()).isEqualTo(CommonStatusEnum.ENABLED.getStatus());
        assertThat(ruleCaptor.getValue().getSerialResetCycle())
                .isEqualTo(BarcodeSerialResetCycleEnum.DAILY.getCycle());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BarcodeRuleItemEntity>> itemsCaptor =
                ArgumentCaptor.forClass((Class) List.class);
        verify(barcodeRuleItemRepository).saveAll(itemsCaptor.capture());
        assertThat(itemsCaptor.getValue()).hasSize(3)
                .allSatisfy(item -> assertThat(item.getRuleId()).isEqualTo(RULE_ID));
    }

    @Test
    @DisplayName("创建规则：条码类型停用时拒绝")
    void createBarcodeRuleRejectsDisabledType() {
        BarcodeTypeEntity disabledType = buildEnabledType();
        disabledType.setStatus(CommonStatusEnum.DISABLED.getStatus());
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(disabledType));

        assertThatThrownBy(() -> barcodeRuleService.createBarcodeRule(buildSaveReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_TYPE_NOT_AVAILABLE));
        verify(barcodeRuleRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("创建规则：组成配置不合法时拒绝并携带逐条错误")
    void createBarcodeRuleRejectsInvalidConfig() {
        BarcodeRuleSaveReqVO reqVO = buildSaveReqVO();
        // 去掉流水号组成项使配置非法
        reqVO.setItems(List.of(reqVO.getItems().get(0)));
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(buildEnabledType()));

        assertThatThrownBy(() -> barcodeRuleService.createBarcodeRule(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> {
                    assertThat(e.getErrorCode())
                            .isEqualTo(BarcodeErrorCodeConstants.BARCODE_RULE_CONFIG_INVALID);
                    assertThat(e.getMessage()).contains("流水号");
                });
        verify(barcodeRuleRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("创建规则：编码已存在时报重复")
    void createBarcodeRuleRejectsDuplicateCode() {
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(buildEnabledType()));
        when(barcodeRuleRepository.existsByRuleCodeAndDeletedFalse("RULE01")).thenReturn(true);

        assertThatThrownBy(() -> barcodeRuleService.createBarcodeRule(buildSaveReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_RULE_CODE_DUPLICATE));
    }

    @Test
    @DisplayName("修改规则：CAS 更新基础信息并整体重写组成明细")
    void updateBarcodeRuleRewritesItems() {
        BarcodeRuleSaveReqVO reqVO = buildSaveReqVO();
        when(barcodeRuleRepository.findByIdAndDeletedFalse(RULE_ID))
                .thenReturn(Optional.of(buildRuleEntity()));
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(buildEnabledType()));
        when(barcodeRuleRepository.existsByRuleCodeAndIdNotAndDeletedFalse("RULE01", RULE_ID))
                .thenReturn(false);
        when(barcodeRuleRepository.updateInfo(RULE_ID, "RULE01", "批次码规则", TYPE_ID, 4,
                BarcodeSerialResetCycleEnum.DAILY.getCycle())).thenReturn(1);

        barcodeRuleService.updateBarcodeRule(RULE_ID, reqVO);

        verify(barcodeRuleItemRepository).logicDeleteByRuleId(RULE_ID);
        verify(barcodeRuleItemRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("修改规则：CAS 未命中(并发删除)时报不存在")
    void updateBarcodeRuleRejectsConcurrentDelete() {
        when(barcodeRuleRepository.findByIdAndDeletedFalse(RULE_ID))
                .thenReturn(Optional.of(buildRuleEntity()));
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(buildEnabledType()));
        when(barcodeRuleRepository.existsByRuleCodeAndIdNotAndDeletedFalse("RULE01", RULE_ID))
                .thenReturn(false);
        when(barcodeRuleRepository.updateInfo(RULE_ID, "RULE01", "批次码规则", TYPE_ID, 4,
                BarcodeSerialResetCycleEnum.DAILY.getCycle())).thenReturn(0);

        assertThatThrownBy(() -> barcodeRuleService.updateBarcodeRule(RULE_ID, buildSaveReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_RULE_NOT_EXISTS));
        verify(barcodeRuleItemRepository, never()).logicDeleteByRuleId(any());
    }

    @Test
    @DisplayName("启用规则：已启用时报状态错误")
    void enableBarcodeRuleRejectsAlreadyEnabled() {
        when(barcodeRuleRepository.updateStatus(RULE_ID, CommonStatusEnum.DISABLED.getStatus(),
                CommonStatusEnum.ENABLED.getStatus())).thenReturn(0);
        when(barcodeRuleRepository.findByIdAndDeletedFalse(RULE_ID))
                .thenReturn(Optional.of(buildRuleEntity()));

        assertThatThrownBy(() -> barcodeRuleService.enableBarcodeRule(RULE_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_RULE_ALREADY_ENABLED));
    }

    @Test
    @DisplayName("停用规则：启用状态 CAS 命中")
    void disableBarcodeRuleTransitionsStatus() {
        when(barcodeRuleRepository.updateStatus(RULE_ID, CommonStatusEnum.ENABLED.getStatus(),
                CommonStatusEnum.DISABLED.getStatus())).thenReturn(1);

        barcodeRuleService.disableBarcodeRule(RULE_ID);

        verify(barcodeRuleRepository).updateStatus(RULE_ID, CommonStatusEnum.ENABLED.getStatus(),
                CommonStatusEnum.DISABLED.getStatus());
    }

    @Test
    @DisplayName("删除规则：被应用规则引用时拒绝")
    void deleteBarcodeRuleRejectsApplyRuleReference() {
        when(barcodeRuleRepository.findByIdAndDeletedFalse(RULE_ID))
                .thenReturn(Optional.of(buildRuleEntity()));
        when(barcodeApplyRuleRepository.existsByRuleIdAndDeletedFalse(RULE_ID)).thenReturn(true);

        assertThatThrownBy(() -> barcodeRuleService.deleteBarcodeRule(RULE_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_RULE_IN_USE_NOT_DELETE));
        verify(barcodeRuleRepository, never()).logicDeleteById(any());
    }

    @Test
    @DisplayName("删除规则：已产生流水(生成过条码)时拒绝")
    void deleteBarcodeRuleRejectsGeneratedSerial() {
        when(barcodeRuleRepository.findByIdAndDeletedFalse(RULE_ID))
                .thenReturn(Optional.of(buildRuleEntity()));
        when(barcodeApplyRuleRepository.existsByRuleIdAndDeletedFalse(RULE_ID)).thenReturn(false);
        when(barcodeSerialRepository.existsByRuleIdAndDeletedFalse(RULE_ID)).thenReturn(true);

        assertThatThrownBy(() -> barcodeRuleService.deleteBarcodeRule(RULE_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_RULE_IN_USE_NOT_DELETE));
    }

    @Test
    @DisplayName("删除规则：未使用时逻辑删除规则与明细")
    void deleteBarcodeRuleLogicallyDeletesRuleAndItems() {
        when(barcodeRuleRepository.findByIdAndDeletedFalse(RULE_ID))
                .thenReturn(Optional.of(buildRuleEntity()));
        when(barcodeApplyRuleRepository.existsByRuleIdAndDeletedFalse(RULE_ID)).thenReturn(false);
        when(barcodeSerialRepository.existsByRuleIdAndDeletedFalse(RULE_ID)).thenReturn(false);
        when(barcodeRuleRepository.logicDeleteById(RULE_ID)).thenReturn(1);

        barcodeRuleService.deleteBarcodeRule(RULE_ID);

        verify(barcodeRuleRepository).logicDeleteById(RULE_ID);
        verify(barcodeRuleItemRepository).logicDeleteByRuleId(RULE_ID);
    }

    @Test
    @DisplayName("查询详情：返回规则与按 seq 升序的组成明细")
    void getBarcodeRuleReturnsItems() {
        when(barcodeRuleRepository.findByIdAndDeletedFalse(RULE_ID))
                .thenReturn(Optional.of(buildRuleEntity()));
        BarcodeRuleItemEntity item = new BarcodeRuleItemEntity();
        item.setId(1L);
        item.setRuleId(RULE_ID);
        item.setSeq(1);
        item.setItemType(BarcodeRuleItemTypeEnum.SERIAL.getType());
        when(barcodeRuleItemRepository.findByRuleIdAndDeletedFalseOrderBySeqAsc(RULE_ID))
                .thenReturn(List.of(item));

        BarcodeRuleRespVO respVO = barcodeRuleService.getBarcodeRule(RULE_ID);

        assertThat(respVO.getRuleCode()).isEqualTo("RULE01");
        assertThat(respVO.getItems()).hasSize(1);
        assertThat(respVO.getItems().get(0).getItemType())
                .isEqualTo(BarcodeRuleItemTypeEnum.SERIAL.getType());
    }

    @Test
    @DisplayName("分页查询：总数为 0 时直接返回空页(SQL-005)")
    void getBarcodeRulePageShortCircuitsOnZeroTotal() {
        when(barcodeRuleRepository.count(any(Specification.class))).thenReturn(0L);

        PageResult<BarcodeRuleRespVO> result =
                barcodeRuleService.getBarcodeRulePage(new BarcodeRulePageReqVO());

        assertThat(result.getTotal()).isZero();
        assertThat(result.getList()).isEmpty();
    }

    @Test
    @DisplayName("预览：样例流水取 1，返回分段结果与容量，不触碰数据库")
    void previewBarcodeRuleComposesSampleValue() {
        BarcodeRulePreviewReqVO reqVO = new BarcodeRulePreviewReqVO();
        reqVO.setSerialLength(4);
        reqVO.setItems(buildItemReqVOs());
        reqVO.setSampleProductCode("YMQ01");

        BarcodeRulePreviewRespVO respVO = barcodeRuleService.previewBarcodeRule(reqVO);

        assertThat(respVO.getBarcodeValue()).startsWith("YMQ01").endsWith("0001");
        assertThat(respVO.getSerialCapacity()).isEqualTo(9999L);
        assertThat(respVO.getSegments()).hasSize(3);
        assertThat(respVO.getTotalLength()).isEqualTo(respVO.getBarcodeValue().length());
    }

    @Test
    @DisplayName("预览：配置不合法时报配置错误")
    void previewBarcodeRuleRejectsInvalidConfig() {
        BarcodeRulePreviewReqVO reqVO = new BarcodeRulePreviewReqVO();
        reqVO.setSerialLength(4);
        reqVO.setItems(List.of(buildItemReqVOs().get(0)));

        assertThatThrownBy(() -> barcodeRuleService.previewBarcodeRule(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_RULE_CONFIG_INVALID));
    }

    @Test
    @DisplayName("预览：含产品编码变量但缺少样例值时报变量缺值")
    void previewBarcodeRuleRejectsMissingSampleVariable() {
        BarcodeRulePreviewReqVO reqVO = new BarcodeRulePreviewReqVO();
        reqVO.setSerialLength(4);
        reqVO.setItems(buildItemReqVOs());

        assertThatThrownBy(() -> barcodeRuleService.previewBarcodeRule(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_RULE_VARIABLE_MISSING));
    }

    @Test
    @DisplayName("校验：返回逐条错误而不抛业务异常")
    void validateBarcodeRuleReturnsErrors() {
        BarcodeRuleValidateReqVO reqVO = new BarcodeRuleValidateReqVO();
        reqVO.setSerialLength(4);
        reqVO.setItems(List.of(buildItemReqVOs().get(0)));

        BarcodeRuleValidateRespVO respVO = barcodeRuleService.validateBarcodeRule(reqVO);

        assertThat(respVO.getValid()).isFalse();
        assertThat(respVO.getErrors()).isNotEmpty();
    }

    /**
     * 构造合法保存请求：产品编码 + 日期 + 流水号。
     *
     * @return 保存请求 VO
     */
    private BarcodeRuleSaveReqVO buildSaveReqVO() {
        BarcodeRuleSaveReqVO reqVO = new BarcodeRuleSaveReqVO();
        reqVO.setRuleCode("RULE01");
        reqVO.setRuleName("批次码规则");
        reqVO.setBarcodeTypeId(TYPE_ID);
        reqVO.setSerialLength(4);
        reqVO.setSerialResetCycle(BarcodeSerialResetCycleEnum.DAILY.getCycle());
        reqVO.setItems(buildItemReqVOs());
        return reqVO;
    }

    /**
     * 构造"产品编码+日期+流水号"组成项请求列表。
     *
     * @return 组成项请求列表
     */
    private List<BarcodeRuleItemSaveReqVO> buildItemReqVOs() {
        BarcodeRuleItemSaveReqVO productItem = new BarcodeRuleItemSaveReqVO();
        productItem.setSeq(1);
        productItem.setItemType(BarcodeRuleItemTypeEnum.VARIABLE.getType());
        productItem.setItemValue("productCode");

        BarcodeRuleItemSaveReqVO dateItem = new BarcodeRuleItemSaveReqVO();
        dateItem.setSeq(2);
        dateItem.setItemType(BarcodeRuleItemTypeEnum.DATE.getType());
        dateItem.setDateFormat("yyyyMMdd");

        BarcodeRuleItemSaveReqVO serialItem = new BarcodeRuleItemSaveReqVO();
        serialItem.setSeq(3);
        serialItem.setItemType(BarcodeRuleItemTypeEnum.SERIAL.getType());

        return List.of(productItem, dateItem, serialItem);
    }

    /**
     * 构造启用状态的条码类型实体。
     *
     * @return 类型实体
     */
    private BarcodeTypeEntity buildEnabledType() {
        BarcodeTypeEntity entity = new BarcodeTypeEntity();
        entity.setId(TYPE_ID);
        entity.setTypeCode("PRODUCT");
        entity.setStatus(CommonStatusEnum.ENABLED.getStatus());
        entity.setDeleted(false);
        return entity;
    }

    /**
     * 构造启用状态的规则实体。
     *
     * @return 规则实体
     */
    private BarcodeRuleEntity buildRuleEntity() {
        BarcodeRuleEntity entity = new BarcodeRuleEntity();
        entity.setId(RULE_ID);
        entity.setRuleCode("RULE01");
        entity.setRuleName("批次码规则");
        entity.setBarcodeTypeId(TYPE_ID);
        entity.setSerialLength(4);
        entity.setSerialResetCycle(BarcodeSerialResetCycleEnum.DAILY.getCycle());
        entity.setStatus(CommonStatusEnum.ENABLED.getStatus());
        entity.setDeleted(false);
        return entity;
    }
}
