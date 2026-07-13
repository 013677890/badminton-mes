package com.badminton.mes.module.barcode.service.impl;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.barcode.constants.BarcodeErrorCodeConstants;
import com.badminton.mes.module.barcode.controller.vo.BarcodeBatchGenerateReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeCancelReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeGenerateReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeGenerateRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeParseReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeParseRespVO;
import com.badminton.mes.module.barcode.dal.entity.BarcodeApplyRuleEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeRuleEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeRuleItemEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeTemplateEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeTypeEntity;
import com.badminton.mes.module.barcode.dal.entity.ProductRefEntity;
import com.badminton.mes.module.barcode.dal.entity.WorkOrderRefEntity;
import com.badminton.mes.module.barcode.dal.redis.BarcodeSerialSequence;
import com.badminton.mes.module.barcode.dal.repository.BarcodeApplyRuleRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeRuleItemRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeRuleRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeSerialRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeTemplateRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeTypeRepository;
import com.badminton.mes.module.barcode.dal.repository.MaterialRefRepository;
import com.badminton.mes.module.barcode.dal.repository.ProductRefRepository;
import com.badminton.mes.module.barcode.dal.repository.WorkOrderRefRepository;
import com.badminton.mes.module.barcode.enums.BarcodeApplyObjectTypeEnum;
import com.badminton.mes.module.barcode.enums.BarcodeModeEnum;
import com.badminton.mes.module.barcode.enums.BarcodeRuleItemTypeEnum;
import com.badminton.mes.module.barcode.enums.BarcodeSerialResetCycleEnum;
import com.badminton.mes.module.barcode.enums.BarcodeSourceTypeEnum;
import com.badminton.mes.module.barcode.enums.BarcodeStatusEnum;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link BarcodeInstanceServiceImpl} 单元测试。
 *
 * <p>数据库、Redis 依赖全部 Mock。覆盖规则生成主路径、撞码重试、
 * 传入值生成、批量生成、作废状态机、解析上下文与工单数据范围。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@ExtendWith(MockitoExtension.class)
class BarcodeInstanceServiceImplTest {

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

    /** 测试用条码 id */
    private static final Long BARCODE_ID = 500L;

    /** 测试用工单 id */
    private static final Long WORK_ORDER_ID = 600L;

    /** 测试用登录用户 id */
    private static final Long OPERATOR_ID = 9L;

    /** 测试用登录用户车间 id */
    private static final Long WORKSHOP_ID = 20L;

    @Mock
    private BarcodeRepository barcodeRepository;

    @Mock
    private BarcodeApplyRuleRepository barcodeApplyRuleRepository;

    @Mock
    private BarcodeTypeRepository barcodeTypeRepository;

    @Mock
    private BarcodeRuleRepository barcodeRuleRepository;

    @Mock
    private BarcodeRuleItemRepository barcodeRuleItemRepository;

    @Mock
    private BarcodeTemplateRepository barcodeTemplateRepository;

    @Mock
    private BarcodeSerialRepository barcodeSerialRepository;

    @Mock
    private com.badminton.mes.module.barcode.dal.repository.BarcodePrintRecordRepository barcodePrintRecordRepository;

    @Mock
    private com.badminton.mes.module.barcode.dal.repository.BarcodeTemplateFieldRepository barcodeTemplateFieldRepository;

    @Mock
    private com.badminton.mes.module.barcode.dal.repository.BarcodeUseRecordRepository barcodeUseRecordRepository;

    @Mock
    private ProductRefRepository productRefRepository;

    @Mock
    private MaterialRefRepository materialRefRepository;

    @Mock
    private WorkOrderRefRepository workOrderRefRepository;

    @Mock
    private BarcodeSerialSequence barcodeSerialSequence;

    private BarcodeInstanceServiceImpl barcodeInstanceService;

    @BeforeEach
    void setUp() {
        barcodeInstanceService = new BarcodeInstanceServiceImpl(barcodeRepository,
                barcodeApplyRuleRepository, barcodeTypeRepository, barcodeRuleRepository,
                barcodeRuleItemRepository, barcodeTemplateRepository, barcodeSerialRepository,
                barcodePrintRecordRepository, barcodeTemplateFieldRepository, barcodeUseRecordRepository,
                productRefRepository, materialRefRepository, workOrderRefRepository,
                barcodeSerialSequence, new tools.jackson.databind.ObjectMapper());
        loginAs(RoleCodeConstants.PMC);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("规则生成：取号组合落库，批次码缺省批次号取条码值，流水事实同步推进")
    void generateBarcodeComposesAndPersists() {
        stubRuleGenerationHappyPath();
        when(barcodeSerialSequence.next(RULE_ID, expectedScope(), BarcodeSerialResetCycleEnum.DAILY))
                .thenReturn(1L);
        when(barcodeRepository.existsByBarcodeValueAndDeletedFalse(anyString())).thenReturn(false);
        stubBarcodeSave();
        when(barcodeSerialRepository.advanceSerial(RULE_ID, expectedScope(), 1L)).thenReturn(1);

        BarcodeGenerateRespVO respVO = barcodeInstanceService.generateBarcode(buildGenerateReqVO());

        assertThat(respVO.getId()).isEqualTo(BARCODE_ID);
        assertThat(respVO.getBarcodeValue()).startsWith("YMQ01").endsWith("0001");
        ArgumentCaptor<BarcodeEntity> captor = ArgumentCaptor.forClass(BarcodeEntity.class);
        verify(barcodeRepository).saveAndFlush(captor.capture());
        BarcodeEntity saved = captor.getValue();
        assertThat(saved.getBatchNo()).isEqualTo(saved.getBarcodeValue());
        assertThat(saved.getBarcodeStatus()).isEqualTo(BarcodeStatusEnum.UNUSED.getStatus());
        assertThat(saved.getSourceType()).isEqualTo(BarcodeSourceTypeEnum.RULE_GENERATE.getType());
        assertThat(saved.getCreateBy()).isEqualTo(OPERATOR_ID);
        verify(barcodeSerialRepository).advanceSerial(RULE_ID, expectedScope(), 1L);
    }

    @Test
    @DisplayName("规则生成：预检撞码时换下一流水号重试")
    void generateBarcodeRetriesOnDuplicateValue() {
        stubRuleGenerationHappyPath();
        when(barcodeSerialSequence.next(RULE_ID, expectedScope(), BarcodeSerialResetCycleEnum.DAILY))
                .thenReturn(1L, 2L);
        when(barcodeRepository.existsByBarcodeValueAndDeletedFalse(anyString()))
                .thenReturn(true, false);
        stubBarcodeSave();
        when(barcodeSerialRepository.advanceSerial(RULE_ID, expectedScope(), 2L)).thenReturn(1);

        BarcodeGenerateRespVO respVO = barcodeInstanceService.generateBarcode(buildGenerateReqVO());

        assertThat(respVO.getBarcodeValue()).endsWith("0002");
        verify(barcodeSerialSequence, times(2)).next(RULE_ID, expectedScope(),
                BarcodeSerialResetCycleEnum.DAILY);
    }

    @Test
    @DisplayName("规则生成：重试耗尽后报生成冲突")
    void generateBarcodeRejectsWhenRetriesExhausted() {
        stubRuleGenerationHappyPath();
        when(barcodeSerialSequence.next(any(), anyString(), any())).thenReturn(1L, 2L, 3L);
        when(barcodeRepository.existsByBarcodeValueAndDeletedFalse(anyString())).thenReturn(true);

        assertThatThrownBy(() -> barcodeInstanceService.generateBarcode(buildGenerateReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_GENERATE_CONFLICT));
        verify(barcodeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("生成：应用规则停用时拒绝")
    void generateBarcodeRejectsDisabledApplyRule() {
        BarcodeApplyRuleEntity disabled = buildApplyRule(BarcodeSourceTypeEnum.RULE_GENERATE.getType());
        disabled.setStatus(CommonStatusEnum.DISABLED.getStatus());
        when(barcodeApplyRuleRepository.findByIdAndDeletedFalse(APPLY_RULE_ID))
                .thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> barcodeInstanceService.generateBarcode(buildGenerateReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_NOT_AVAILABLE));
    }

    @Test
    @DisplayName("生成：外部导入来源的应用规则不能在线生成")
    void generateBarcodeRejectsImportSource() {
        when(barcodeApplyRuleRepository.findByIdAndDeletedFalse(APPLY_RULE_ID))
                .thenReturn(Optional.of(buildApplyRule(BarcodeSourceTypeEnum.EXTERNAL_IMPORT.getType())));

        assertThatThrownBy(() -> barcodeInstanceService.generateBarcode(buildGenerateReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_SOURCE_NOT_GENERATE));
    }

    @Test
    @DisplayName("传入值生成：缺少条码值时拒绝")
    void generateBarcodeRejectsMissingInputValue() {
        stubInputValueApplyRule();

        assertThatThrownBy(() -> barcodeInstanceService.generateBarcode(buildGenerateReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_INPUT_VALUE_REQUIRED));
    }

    @Test
    @DisplayName("传入值生成：登记提交的条码值，不调用发号器")
    void generateBarcodePersistsInputValue() {
        stubInputValueApplyRule();
        when(barcodeRepository.existsByBarcodeValueAndDeletedFalse("EXT-0001")).thenReturn(false);
        stubBarcodeSave();

        BarcodeGenerateReqVO reqVO = buildGenerateReqVO();
        reqVO.setInputBarcodeValue("EXT-0001");
        BarcodeGenerateRespVO respVO = barcodeInstanceService.generateBarcode(reqVO);

        assertThat(respVO.getBarcodeValue()).isEqualTo("EXT-0001");
        org.mockito.Mockito.verifyNoInteractions(barcodeSerialSequence);
    }

    @Test
    @DisplayName("传入值生成：条码值已存在时报重复")
    void generateBarcodeRejectsDuplicateInputValue() {
        stubInputValueApplyRule();
        when(barcodeRepository.existsByBarcodeValueAndDeletedFalse("EXT-0001")).thenReturn(true);

        BarcodeGenerateReqVO reqVO = buildGenerateReqVO();
        reqVO.setInputBarcodeValue("EXT-0001");

        assertThatThrownBy(() -> barcodeInstanceService.generateBarcode(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_VALUE_DUPLICATE));
    }

    @Test
    @DisplayName("生成：非管理员关联跨车间工单时按无可见数据拒绝")
    void generateBarcodeRejectsWorkOrderOutOfScope() {
        stubRuleGenerationHappyPath();
        WorkOrderRefEntity workOrder = buildWorkOrder();
        workOrder.setWorkshopId(999L);
        when(workOrderRefRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(workOrder));

        BarcodeGenerateReqVO reqVO = buildGenerateReqVO();
        reqVO.setWorkOrderId(WORK_ORDER_ID);

        assertThatThrownBy(() -> barcodeInstanceService.generateBarcode(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_WORK_ORDER_NOT_AVAILABLE));
    }

    @Test
    @DisplayName("生成：管理员不受车间范围限制，但工单产品不一致仍拒绝")
    void generateBarcodeRejectsWorkOrderProductMismatchForAdmin() {
        loginAs(RoleCodeConstants.ADMIN);
        stubRuleGenerationHappyPath();
        WorkOrderRefEntity workOrder = buildWorkOrder();
        workOrder.setWorkshopId(999L);
        workOrder.setProductId(888L);
        when(workOrderRefRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(workOrder));

        BarcodeGenerateReqVO reqVO = buildGenerateReqVO();
        reqVO.setWorkOrderId(WORK_ORDER_ID);

        assertThatThrownBy(() -> barcodeInstanceService.generateBarcode(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_WORK_ORDER_PRODUCT_MISMATCH));
    }

    @Test
    @DisplayName("批量生成：逐个取号落库，返回同数量结果")
    void batchGenerateBarcodesGeneratesEach() {
        stubRuleGenerationHappyPath();
        when(barcodeSerialSequence.next(RULE_ID, expectedScope(), BarcodeSerialResetCycleEnum.DAILY))
                .thenReturn(1L, 2L, 3L);
        when(barcodeRepository.existsByBarcodeValueAndDeletedFalse(anyString())).thenReturn(false);
        stubBarcodeSave();
        when(barcodeSerialRepository.advanceSerial(any(), anyString(), any())).thenReturn(1);

        BarcodeBatchGenerateReqVO reqVO = new BarcodeBatchGenerateReqVO();
        reqVO.setApplyRuleId(APPLY_RULE_ID);
        reqVO.setQuantity(3);

        List<BarcodeGenerateRespVO> results = barcodeInstanceService.batchGenerateBarcodes(reqVO);

        assertThat(results).hasSize(3);
        assertThat(results.get(2).getBarcodeValue()).endsWith("0003");
        verify(barcodeSerialSequence, times(3)).next(RULE_ID, expectedScope(),
                BarcodeSerialResetCycleEnum.DAILY);
    }

    @Test
    @DisplayName("批量生成：传入值生成来源不支持批量")
    void batchGenerateBarcodesRejectsInputValueSource() {
        stubInputValueApplyRule();
        BarcodeBatchGenerateReqVO reqVO = new BarcodeBatchGenerateReqVO();
        reqVO.setApplyRuleId(APPLY_RULE_ID);
        reqVO.setQuantity(2);
        reqVO.setInputBarcodeValue("EXT-0001");

        assertThatThrownBy(() -> barcodeInstanceService.batchGenerateBarcodes(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_BATCH_NOT_SUPPORT_INPUT_VALUE));
    }

    @Test
    @DisplayName("解析：返回条码事实与产品/类型上下文")
    void parseBarcodeReturnsBusinessContext() {
        BarcodeEntity barcode = buildBarcode(BarcodeStatusEnum.UNUSED.getStatus());
        when(barcodeRepository.findByBarcodeValueAndDeletedFalse("YMQ01202607120001"))
                .thenReturn(Optional.of(barcode));
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(buildType()));
        when(productRefRepository.findByIdAndDeletedFalse(PRODUCT_ID))
                .thenReturn(Optional.of(buildProduct()));

        BarcodeParseReqVO reqVO = new BarcodeParseReqVO();
        reqVO.setBarcodeValue("YMQ01202607120001");
        BarcodeParseRespVO respVO = barcodeInstanceService.parseBarcode(reqVO);

        assertThat(respVO.getId()).isEqualTo(BARCODE_ID);
        assertThat(respVO.getBarcodeTypeCode()).isEqualTo("PRODUCT");
        assertThat(respVO.getProductCode()).isEqualTo("YMQ01");
        assertThat(respVO.getProductName()).isEqualTo("羽毛球A级");
    }

    @Test
    @DisplayName("解析：条码不存在时报不存在")
    void parseBarcodeRejectsUnknownValue() {
        when(barcodeRepository.findByBarcodeValueAndDeletedFalse("UNKNOWN"))
                .thenReturn(Optional.empty());

        BarcodeParseReqVO reqVO = new BarcodeParseReqVO();
        reqVO.setBarcodeValue("UNKNOWN");

        assertThatThrownBy(() -> barcodeInstanceService.parseBarcode(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_NOT_EXISTS));
    }

    @Test
    @DisplayName("作废：未使用条码 CAS 命中")
    void cancelBarcodeTransitionsUnusedToCancelled() {
        when(barcodeRepository.updateStatus(BARCODE_ID, BarcodeStatusEnum.UNUSED.getStatus(),
                BarcodeStatusEnum.CANCELLED.getStatus())).thenReturn(1);

        barcodeInstanceService.cancelBarcode(BARCODE_ID, new BarcodeCancelReqVO());

        verify(barcodeRepository).updateStatus(BARCODE_ID, BarcodeStatusEnum.UNUSED.getStatus(),
                BarcodeStatusEnum.CANCELLED.getStatus());
    }

    @Test
    @DisplayName("作废：已使用条码不能作废(已冻结决策)")
    void cancelBarcodeRejectsUsedBarcode() {
        when(barcodeRepository.updateStatus(BARCODE_ID, BarcodeStatusEnum.UNUSED.getStatus(),
                BarcodeStatusEnum.CANCELLED.getStatus())).thenReturn(0);
        when(barcodeRepository.findByIdAndDeletedFalse(BARCODE_ID))
                .thenReturn(Optional.of(buildBarcode(BarcodeStatusEnum.USED.getStatus())));

        assertThatThrownBy(() -> barcodeInstanceService.cancelBarcode(BARCODE_ID, new BarcodeCancelReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_USED_NOT_CANCEL));
    }

    @Test
    @DisplayName("作废：已作废条码重复作废时报状态错误")
    void cancelBarcodeRejectsAlreadyCancelled() {
        when(barcodeRepository.updateStatus(BARCODE_ID, BarcodeStatusEnum.UNUSED.getStatus(),
                BarcodeStatusEnum.CANCELLED.getStatus())).thenReturn(0);
        when(barcodeRepository.findByIdAndDeletedFalse(BARCODE_ID))
                .thenReturn(Optional.of(buildBarcode(BarcodeStatusEnum.CANCELLED.getStatus())));

        assertThatThrownBy(() -> barcodeInstanceService.cancelBarcode(BARCODE_ID, new BarcodeCancelReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_ALREADY_CANCELLED));
    }

    @Test
    @DisplayName("流水事实：维度记录不存在时插入首条记录")
    void generateBarcodeInsertsSerialRecordWhenAbsent() {
        stubRuleGenerationHappyPath();
        when(barcodeSerialSequence.next(RULE_ID, expectedScope(), BarcodeSerialResetCycleEnum.DAILY))
                .thenReturn(1L);
        when(barcodeRepository.existsByBarcodeValueAndDeletedFalse(anyString())).thenReturn(false);
        stubBarcodeSave();
        when(barcodeSerialRepository.advanceSerial(RULE_ID, expectedScope(), 1L)).thenReturn(0);
        when(barcodeSerialRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        barcodeInstanceService.generateBarcode(buildGenerateReqVO());

        verify(barcodeSerialRepository, atLeastOnce()).saveAndFlush(any());
    }

    @Test
    @DisplayName("打印：首次打印插入序号 1 记录并保存模板版本与预览快照")
    void printBarcodeInsertsFirstRecordWithSnapshot() {
        when(barcodeRepository.findByIdAndDeletedFalse(BARCODE_ID))
                .thenReturn(Optional.of(buildBarcode(BarcodeStatusEnum.UNUSED.getStatus())));
        stubPrintTemplate();
        when(barcodePrintRecordRepository.findFirstByBarcodeIdAndDeletedFalseOrderByPrintCountDesc(
                BARCODE_ID)).thenReturn(Optional.empty());
        when(barcodePrintRecordRepository.saveAndFlush(
                any(com.badminton.mes.module.barcode.dal.entity.BarcodePrintRecordEntity.class)))
                .thenAnswer(invocation -> {
                    com.badminton.mes.module.barcode.dal.entity.BarcodePrintRecordEntity record =
                            invocation.getArgument(0);
                    record.setId(700L);
                    return record;
                });

        com.badminton.mes.module.barcode.controller.vo.BarcodePrintRespVO respVO =
                barcodeInstanceService.printBarcode(BARCODE_ID,
                        new com.badminton.mes.module.barcode.controller.vo.BarcodePrintReqVO());

        assertThat(respVO.getPrintRecordId()).isEqualTo(700L);
        assertThat(respVO.getPrintCount()).isEqualTo(1);
        assertThat(respVO.getTemplateVersion()).isEqualTo("V1");
        ArgumentCaptor<com.badminton.mes.module.barcode.dal.entity.BarcodePrintRecordEntity> captor =
                ArgumentCaptor.forClass(
                        com.badminton.mes.module.barcode.dal.entity.BarcodePrintRecordEntity.class);
        verify(barcodePrintRecordRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPreviewContent()).contains("YMQ01202607120001");
        assertThat(captor.getValue().getPrintBy()).isEqualTo(OPERATOR_ID);
        // 打印是派生属性，不改变条码持久状态
        verify(barcodeRepository, never()).updateStatus(any(), any(), any());
    }

    @Test
    @DisplayName("打印：重复打印缺少原因时拒绝")
    void printBarcodeRejectsReprintWithoutReason() {
        when(barcodeRepository.findByIdAndDeletedFalse(BARCODE_ID))
                .thenReturn(Optional.of(buildBarcode(BarcodeStatusEnum.UNUSED.getStatus())));
        stubPrintTemplate();
        com.badminton.mes.module.barcode.dal.entity.BarcodePrintRecordEntity lastRecord =
                new com.badminton.mes.module.barcode.dal.entity.BarcodePrintRecordEntity();
        lastRecord.setPrintCount(1);
        when(barcodePrintRecordRepository.findFirstByBarcodeIdAndDeletedFalseOrderByPrintCountDesc(
                BARCODE_ID)).thenReturn(Optional.of(lastRecord));

        assertThatThrownBy(() -> barcodeInstanceService.printBarcode(BARCODE_ID,
                new com.badminton.mes.module.barcode.controller.vo.BarcodePrintReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_REPRINT_REASON_REQUIRED));
        verify(barcodePrintRecordRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("打印：重复打印携带原因时序号递增")
    void printBarcodeIncrementsPrintCountOnReprint() {
        when(barcodeRepository.findByIdAndDeletedFalse(BARCODE_ID))
                .thenReturn(Optional.of(buildBarcode(BarcodeStatusEnum.USED.getStatus())));
        stubPrintTemplate();
        com.badminton.mes.module.barcode.dal.entity.BarcodePrintRecordEntity lastRecord =
                new com.badminton.mes.module.barcode.dal.entity.BarcodePrintRecordEntity();
        lastRecord.setPrintCount(2);
        when(barcodePrintRecordRepository.findFirstByBarcodeIdAndDeletedFalseOrderByPrintCountDesc(
                BARCODE_ID)).thenReturn(Optional.of(lastRecord));
        when(barcodePrintRecordRepository.saveAndFlush(any(
                com.badminton.mes.module.barcode.dal.entity.BarcodePrintRecordEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        com.badminton.mes.module.barcode.controller.vo.BarcodePrintReqVO reqVO =
                new com.badminton.mes.module.barcode.controller.vo.BarcodePrintReqVO();
        reqVO.setReason("标签破损重打");
        com.badminton.mes.module.barcode.controller.vo.BarcodePrintRespVO respVO =
                barcodeInstanceService.printBarcode(BARCODE_ID, reqVO);

        assertThat(respVO.getPrintCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("打印：已作废条码不能打印")
    void printBarcodeRejectsCancelledBarcode() {
        when(barcodeRepository.findByIdAndDeletedFalse(BARCODE_ID))
                .thenReturn(Optional.of(buildBarcode(BarcodeStatusEnum.CANCELLED.getStatus())));

        assertThatThrownBy(() -> barcodeInstanceService.printBarcode(BARCODE_ID,
                new com.badminton.mes.module.barcode.controller.vo.BarcodePrintReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_CANCELLED_NOT_PRINT));
    }

    @Test
    @DisplayName("导入：合法条目落库为外部导入来源，重复与批内重复逐条记失败原因")
    void importBarcodesPartiallySucceedsWithFailures() {
        BarcodeApplyRuleEntity importRule =
                buildApplyRule(BarcodeSourceTypeEnum.EXTERNAL_IMPORT.getType());
        when(barcodeApplyRuleRepository.findByIdAndDeletedFalse(APPLY_RULE_ID))
                .thenReturn(Optional.of(importRule));
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(buildType()));
        when(barcodeTemplateRepository.findByIdAndDeletedFalse(TEMPLATE_ID))
                .thenReturn(Optional.of(buildTemplate()));
        when(barcodeRepository.existsByBarcodeValueAndDeletedFalse("EXT-001")).thenReturn(false);
        when(barcodeRepository.existsByBarcodeValueAndDeletedFalse("EXT-002")).thenReturn(true);
        stubBarcodeSave();

        com.badminton.mes.module.barcode.controller.vo.BarcodeImportReqVO reqVO =
                new com.badminton.mes.module.barcode.controller.vo.BarcodeImportReqVO();
        reqVO.setApplyRuleId(APPLY_RULE_ID);
        reqVO.setItems(List.of(buildImportItem("EXT-001"), buildImportItem("EXT-002"),
                buildImportItem("EXT-001")));

        com.badminton.mes.module.barcode.controller.vo.BarcodeImportRespVO respVO =
                barcodeInstanceService.importBarcodes(reqVO);

        assertThat(respVO.getTotalCount()).isEqualTo(3);
        assertThat(respVO.getSuccessCount()).isEqualTo(1);
        assertThat(respVO.getFailCount()).isEqualTo(2);
        assertThat(respVO.getFailures()).extracting(
                        com.badminton.mes.module.barcode.controller.vo.BarcodeImportRespVO.Failure::getIndex)
                .containsExactly(1, 2);
        ArgumentCaptor<BarcodeEntity> captor = ArgumentCaptor.forClass(BarcodeEntity.class);
        verify(barcodeRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getSourceType())
                .isEqualTo(BarcodeSourceTypeEnum.EXTERNAL_IMPORT.getType());
        assertThat(captor.getValue().getBatchNo()).isEqualTo("EXT-001");
    }

    @Test
    @DisplayName("导入：应用规则来源不是外部导入时整单拒绝")
    void importBarcodesRejectsNonImportSource() {
        when(barcodeApplyRuleRepository.findByIdAndDeletedFalse(APPLY_RULE_ID))
                .thenReturn(Optional.of(buildApplyRule(BarcodeSourceTypeEnum.RULE_GENERATE.getType())));

        com.badminton.mes.module.barcode.controller.vo.BarcodeImportReqVO reqVO =
                new com.badminton.mes.module.barcode.controller.vo.BarcodeImportReqVO();
        reqVO.setApplyRuleId(APPLY_RULE_ID);
        reqVO.setItems(List.of(buildImportItem("EXT-001")));

        assertThatThrownBy(() -> barcodeInstanceService.importBarcodes(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_SOURCE_NOT_IMPORT));
    }

    @Test
    @DisplayName("导入：条目关联跨车间工单时该条失败，其余继续")
    void importBarcodesRecordsScopeFailurePerItem() {
        BarcodeApplyRuleEntity importRule =
                buildApplyRule(BarcodeSourceTypeEnum.EXTERNAL_IMPORT.getType());
        when(barcodeApplyRuleRepository.findByIdAndDeletedFalse(APPLY_RULE_ID))
                .thenReturn(Optional.of(importRule));
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(buildType()));
        when(barcodeTemplateRepository.findByIdAndDeletedFalse(TEMPLATE_ID))
                .thenReturn(Optional.of(buildTemplate()));
        when(barcodeRepository.existsByBarcodeValueAndDeletedFalse("EXT-001")).thenReturn(false);
        WorkOrderRefEntity foreignOrder = buildWorkOrder();
        foreignOrder.setWorkshopId(999L);
        when(workOrderRefRepository.findByIdAndDeletedFalse(WORK_ORDER_ID))
                .thenReturn(Optional.of(foreignOrder));

        com.badminton.mes.module.barcode.controller.vo.BarcodeImportReqVO reqVO =
                new com.badminton.mes.module.barcode.controller.vo.BarcodeImportReqVO();
        reqVO.setApplyRuleId(APPLY_RULE_ID);
        com.badminton.mes.module.barcode.controller.vo.BarcodeImportReqVO.Item scoped =
                buildImportItem("EXT-001");
        scoped.setWorkOrderId(WORK_ORDER_ID);
        reqVO.setItems(List.of(scoped));

        com.badminton.mes.module.barcode.controller.vo.BarcodeImportRespVO respVO =
                barcodeInstanceService.importBarcodes(reqVO);

        assertThat(respVO.getSuccessCount()).isZero();
        assertThat(respVO.getFailures()).hasSize(1);
        assertThat(respVO.getFailures().get(0).getReason()).contains("授权");
        verify(barcodeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("使用记录：按条码返回扫码轨迹，条码不存在时报不存在")
    void getBarcodeUseRecordsReturnsTrail() {
        when(barcodeRepository.findByIdAndDeletedFalse(BARCODE_ID))
                .thenReturn(Optional.of(buildBarcode(BarcodeStatusEnum.USED.getStatus())));
        com.badminton.mes.module.barcode.dal.entity.BarcodeUseRecordEntity record =
                new com.badminton.mes.module.barcode.dal.entity.BarcodeUseRecordEntity();
        record.setId(800L);
        record.setBarcodeId(BARCODE_ID);
        record.setTaskId(1L);
        record.setProcessId(2L);
        record.setUserId(OPERATOR_ID);
        record.setUseType(1);
        when(barcodeUseRecordRepository.findByBarcodeIdAndDeletedFalseOrderByBusinessTimeDesc(BARCODE_ID))
                .thenReturn(List.of(record));

        List<com.badminton.mes.module.barcode.controller.vo.BarcodeUseRecordRespVO> records =
                barcodeInstanceService.getBarcodeUseRecords(BARCODE_ID);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getId()).isEqualTo(800L);

        when(barcodeRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> barcodeInstanceService.getBarcodeUseRecords(999L))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_NOT_EXISTS));
    }

    /**
     * 构造导入明细项。
     *
     * @param barcodeValue 条码值
     * @return 导入明细项
     */
    private com.badminton.mes.module.barcode.controller.vo.BarcodeImportReqVO.Item buildImportItem(
            String barcodeValue) {
        com.badminton.mes.module.barcode.controller.vo.BarcodeImportReqVO.Item item =
                new com.badminton.mes.module.barcode.controller.vo.BarcodeImportReqVO.Item();
        item.setBarcodeValue(barcodeValue);
        return item;
    }

    /**
     * Mock 打印路径的应用规则、模板与字段。
     */
    private void stubPrintTemplate() {
        lenient().when(barcodeApplyRuleRepository.findByIdAndDeletedFalse(APPLY_RULE_ID))
                .thenReturn(Optional.of(buildApplyRule(BarcodeSourceTypeEnum.RULE_GENERATE.getType())));
        lenient().when(barcodeTemplateRepository.findByIdAndDeletedFalse(TEMPLATE_ID))
                .thenReturn(Optional.of(buildVersionedTemplate()));
        lenient().when(barcodeTemplateFieldRepository
                        .findByTemplateIdAndDeletedFalseOrderByIdAsc(TEMPLATE_ID))
                .thenReturn(List.of(buildBarcodeField()));
        lenient().when(productRefRepository.findByIdAndDeletedFalse(PRODUCT_ID))
                .thenReturn(Optional.of(buildProduct()));
    }

    /**
     * 构造带版本号的启用模板。
     *
     * @return 模板实体
     */
    private BarcodeTemplateEntity buildVersionedTemplate() {
        BarcodeTemplateEntity template = buildTemplate();
        template.setTemplateCode("TPL01");
        template.setVersion("V1");
        return template;
    }

    /**
     * 构造承载条码值的模板字段。
     *
     * @return 字段实体
     */
    private com.badminton.mes.module.barcode.dal.entity.BarcodeTemplateFieldEntity buildBarcodeField() {
        com.badminton.mes.module.barcode.dal.entity.BarcodeTemplateFieldEntity field =
                new com.badminton.mes.module.barcode.dal.entity.BarcodeTemplateFieldEntity();
        field.setId(1L);
        field.setTemplateId(TEMPLATE_ID);
        field.setFieldName("条码值");
        field.setFieldType(
                com.badminton.mes.module.barcode.enums.BarcodeTemplateFieldTypeEnum.BARCODE.getType());
        field.setDataSource("barcodeValue");
        return field;
    }

    /**
     * 构造登录上下文。
     *
     * @param roleCode 角色编码
     */
    private void loginAs(String roleCode) {
        SecurityContextHolder.clear();
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(OPERATOR_ID);
        loginUser.setUserNo("tester");
        loginUser.setWorkshopId(WORKSHOP_ID);
        loginUser.setRoleCodes(List.of(roleCode));
        SecurityContextHolder.set("unit-test-token", loginUser);
    }

    /**
     * Mock 规则生成主路径的档案与规则配置。
     */
    private void stubRuleGenerationHappyPath() {
        when(barcodeApplyRuleRepository.findByIdAndDeletedFalse(APPLY_RULE_ID))
                .thenReturn(Optional.of(buildApplyRule(BarcodeSourceTypeEnum.RULE_GENERATE.getType())));
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(buildType()));
        when(barcodeTemplateRepository.findByIdAndDeletedFalse(TEMPLATE_ID))
                .thenReturn(Optional.of(buildTemplate()));
        when(productRefRepository.findByIdAndDeletedFalse(PRODUCT_ID))
                .thenReturn(Optional.of(buildProduct()));
        lenient().when(barcodeRuleRepository.findByIdAndDeletedFalse(RULE_ID))
                .thenReturn(Optional.of(buildRule()));
        lenient().when(barcodeRuleItemRepository.findByRuleIdAndDeletedFalseOrderBySeqAsc(RULE_ID))
                .thenReturn(buildRuleItems());
    }

    /**
     * Mock 传入值生成来源的应用规则及其档案。
     */
    private void stubInputValueApplyRule() {
        when(barcodeApplyRuleRepository.findByIdAndDeletedFalse(APPLY_RULE_ID))
                .thenReturn(Optional.of(buildApplyRule(BarcodeSourceTypeEnum.INPUT_VALUE.getType())));
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(buildType()));
        when(barcodeTemplateRepository.findByIdAndDeletedFalse(TEMPLATE_ID))
                .thenReturn(Optional.of(buildTemplate()));
        when(productRefRepository.findByIdAndDeletedFalse(PRODUCT_ID))
                .thenReturn(Optional.of(buildProduct()));
    }

    /**
     * Mock 条码落库回填主键。
     */
    private void stubBarcodeSave() {
        when(barcodeRepository.saveAndFlush(any(BarcodeEntity.class))).thenAnswer(invocation -> {
            BarcodeEntity entity = invocation.getArgument(0);
            entity.setId(BARCODE_ID);
            return entity;
        });
    }

    /**
     * 期望的流水作用域：按日周期日期段 + 产品编码。
     *
     * @return 作用域字符串
     */
    private String expectedScope() {
        return BarcodeSerialResetCycleEnum.DAILY.scopeSegment(java.time.LocalDate.now()) + ":YMQ01";
    }

    /**
     * 构造生成请求。
     *
     * @return 生成请求 VO
     */
    private BarcodeGenerateReqVO buildGenerateReqVO() {
        BarcodeGenerateReqVO reqVO = new BarcodeGenerateReqVO();
        reqVO.setApplyRuleId(APPLY_RULE_ID);
        return reqVO;
    }

    /**
     * 构造启用状态的应用规则。
     *
     * @param sourceType 条码来源
     * @return 应用规则实体
     */
    private BarcodeApplyRuleEntity buildApplyRule(Integer sourceType) {
        BarcodeApplyRuleEntity applyRule = new BarcodeApplyRuleEntity();
        applyRule.setId(APPLY_RULE_ID);
        applyRule.setObjectType(BarcodeApplyObjectTypeEnum.PRODUCT.getType());
        applyRule.setProductId(PRODUCT_ID);
        applyRule.setBarcodeTypeId(TYPE_ID);
        applyRule.setBarcodeMode(BarcodeModeEnum.BATCH.getMode());
        applyRule.setRuleId(RULE_ID);
        applyRule.setTemplateId(TEMPLATE_ID);
        applyRule.setSourceType(sourceType);
        applyRule.setDefaultFlag(true);
        applyRule.setStatus(CommonStatusEnum.ENABLED.getStatus());
        applyRule.setDeleted(false);
        return applyRule;
    }

    /**
     * 构造"产品编码+日期+流水号"规则明细。
     *
     * @return 明细实体列表
     */
    private List<BarcodeRuleItemEntity> buildRuleItems() {
        BarcodeRuleItemEntity productItem = new BarcodeRuleItemEntity();
        productItem.setSeq(1);
        productItem.setItemType(BarcodeRuleItemTypeEnum.VARIABLE.getType());
        productItem.setItemValue("productCode");

        BarcodeRuleItemEntity dateItem = new BarcodeRuleItemEntity();
        dateItem.setSeq(2);
        dateItem.setItemType(BarcodeRuleItemTypeEnum.DATE.getType());
        dateItem.setDateFormat("yyyyMMdd");

        BarcodeRuleItemEntity serialItem = new BarcodeRuleItemEntity();
        serialItem.setSeq(3);
        serialItem.setItemType(BarcodeRuleItemTypeEnum.SERIAL.getType());
        return List.of(productItem, dateItem, serialItem);
    }

    /**
     * 构造启用状态的条码规则(按日重置、4 位流水)。
     *
     * @return 规则实体
     */
    private BarcodeRuleEntity buildRule() {
        BarcodeRuleEntity rule = new BarcodeRuleEntity();
        rule.setId(RULE_ID);
        rule.setBarcodeTypeId(TYPE_ID);
        rule.setSerialLength(4);
        rule.setSerialResetCycle(BarcodeSerialResetCycleEnum.DAILY.getCycle());
        rule.setStatus(CommonStatusEnum.ENABLED.getStatus());
        rule.setDeleted(false);
        return rule;
    }

    /**
     * 构造启用状态的条码类型。
     *
     * @return 类型实体
     */
    private BarcodeTypeEntity buildType() {
        BarcodeTypeEntity entity = new BarcodeTypeEntity();
        entity.setId(TYPE_ID);
        entity.setTypeCode("PRODUCT");
        entity.setTypeName("产品码");
        entity.setStatus(CommonStatusEnum.ENABLED.getStatus());
        entity.setDeleted(false);
        return entity;
    }

    /**
     * 构造启用状态的条码模板。
     *
     * @return 模板实体
     */
    private BarcodeTemplateEntity buildTemplate() {
        BarcodeTemplateEntity entity = new BarcodeTemplateEntity();
        entity.setId(TEMPLATE_ID);
        entity.setStatus(CommonStatusEnum.ENABLED.getStatus());
        entity.setDeleted(false);
        return entity;
    }

    /**
     * 构造启用状态的产品只读引用。
     *
     * @return 产品只读引用
     */
    private ProductRefEntity buildProduct() {
        ProductRefEntity product = new ProductRefEntity();
        product.setId(PRODUCT_ID);
        product.setProductCode("YMQ01");
        product.setProductName("羽毛球A级");
        product.setStatus(CommonStatusEnum.ENABLED.getStatus());
        product.setDeleted(false);
        return product;
    }

    /**
     * 构造属于测试车间与产品的工单只读引用。
     *
     * @return 工单只读引用
     */
    private WorkOrderRefEntity buildWorkOrder() {
        WorkOrderRefEntity workOrder = new WorkOrderRefEntity();
        workOrder.setId(WORK_ORDER_ID);
        workOrder.setProductId(PRODUCT_ID);
        workOrder.setWorkshopId(WORKSHOP_ID);
        workOrder.setDeleted(false);
        return workOrder;
    }

    /**
     * 构造指定状态的条码实体。
     *
     * @param status 条码状态
     * @return 条码实体
     */
    private BarcodeEntity buildBarcode(Integer status) {
        BarcodeEntity barcode = new BarcodeEntity();
        barcode.setId(BARCODE_ID);
        barcode.setBarcodeValue("YMQ01202607120001");
        barcode.setBarcodeTypeId(TYPE_ID);
        barcode.setBarcodeMode(BarcodeModeEnum.BATCH.getMode());
        barcode.setApplyRuleId(APPLY_RULE_ID);
        barcode.setProductId(PRODUCT_ID);
        barcode.setBarcodeStatus(status);
        barcode.setDeleted(false);
        return barcode;
    }
}
