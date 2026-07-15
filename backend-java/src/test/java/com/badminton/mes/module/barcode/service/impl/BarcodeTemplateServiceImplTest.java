package com.badminton.mes.module.barcode.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.barcode.constants.BarcodeErrorCodeConstants;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplateFieldSaveReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplatePageReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplatePreviewReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplatePreviewRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplateRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplateSaveReqVO;
import com.badminton.mes.module.barcode.dal.entity.BarcodeTemplateEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeTemplateFieldEntity;
import com.badminton.mes.module.barcode.dal.repository.BarcodeApplyRuleRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeTemplateFieldRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeTemplateRepository;
import com.badminton.mes.module.barcode.enums.BarcodeTemplateFieldTypeEnum;

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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link BarcodeTemplateServiceImpl} 单元测试。
 *
 * <p>数据库依赖全部 Mock。覆盖创建校验、绑定升版本与就地修改两条修改路径、
 * 版本号推进和预览内容解析。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@ExtendWith(MockitoExtension.class)
class BarcodeTemplateServiceImplTest {

    /** 测试用模板 id */
    private static final Long TEMPLATE_ID = 300L;

    @Mock
    private BarcodeTemplateRepository barcodeTemplateRepository;

    @Mock
    private BarcodeTemplateFieldRepository barcodeTemplateFieldRepository;

    @Mock
    private BarcodeApplyRuleRepository barcodeApplyRuleRepository;

    private BarcodeTemplateServiceImpl barcodeTemplateService;

    @BeforeEach
    void setUp() {
        barcodeTemplateService = new BarcodeTemplateServiceImpl(barcodeTemplateRepository,
                barcodeTemplateFieldRepository, barcodeApplyRuleRepository);
    }

    @Test
    @DisplayName("创建模板：初始版本 V1 默认启用并保存字段")
    void createBarcodeTemplateSavesInitialVersion() {
        BarcodeTemplateSaveReqVO reqVO = buildSaveReqVO();
        when(barcodeTemplateRepository.existsByTemplateCodeAndDeletedFalse("TPL01")).thenReturn(false);
        when(barcodeTemplateRepository.saveAndFlush(any(BarcodeTemplateEntity.class)))
                .thenAnswer(invocation -> {
                    BarcodeTemplateEntity entity = invocation.getArgument(0);
                    entity.setId(TEMPLATE_ID);
                    return entity;
                });

        Long id = barcodeTemplateService.createBarcodeTemplate(reqVO);

        assertThat(id).isEqualTo(TEMPLATE_ID);
        ArgumentCaptor<BarcodeTemplateEntity> captor = ArgumentCaptor.forClass(BarcodeTemplateEntity.class);
        verify(barcodeTemplateRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo("V1");
        assertThat(captor.getValue().getStatus()).isEqualTo(CommonStatusEnum.ENABLED.getStatus());
        verify(barcodeTemplateFieldRepository).saveAll(any());
    }

    @Test
    @DisplayName("创建模板：缺少条码/二维码字段时拒绝")
    void createBarcodeTemplateRejectsMissingBarcodeField() {
        BarcodeTemplateSaveReqVO reqVO = buildSaveReqVO();
        reqVO.setFields(List.of(buildFieldReqVO("产品名称", BarcodeTemplateFieldTypeEnum.TEXT.getType(),
                "productName")));

        assertThatThrownBy(() -> barcodeTemplateService.createBarcodeTemplate(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_TEMPLATE_MISSING_BARCODE_FIELD));
        verify(barcodeTemplateRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("创建模板：编码已存在(任意版本)时报重复")
    void createBarcodeTemplateRejectsDuplicateCode() {
        when(barcodeTemplateRepository.existsByTemplateCodeAndDeletedFalse("TPL01")).thenReturn(true);

        assertThatThrownBy(() -> barcodeTemplateService.createBarcodeTemplate(buildSaveReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_TEMPLATE_CODE_DUPLICATE));
    }

    @Test
    @DisplayName("修改模板：未被绑定时就地修改并重写字段，版本不变")
    void updateBarcodeTemplateInPlaceWhenUnbound() {
        when(barcodeTemplateRepository.findByIdAndDeletedFalse(TEMPLATE_ID))
                .thenReturn(Optional.of(buildTemplateEntity("V1")));
        when(barcodeApplyRuleRepository.existsByTemplateIdAndDeletedFalse(TEMPLATE_ID)).thenReturn(false);
        when(barcodeTemplateRepository.updateInfo(anyLong(), anyString(), any(), any())).thenReturn(1);

        barcodeTemplateService.updateBarcodeTemplate(TEMPLATE_ID, buildSaveReqVO());

        verify(barcodeTemplateRepository).updateInfo(TEMPLATE_ID, "产品标签", new BigDecimal("60.00"),
                new BigDecimal("40.00"));
        verify(barcodeTemplateFieldRepository).logicDeleteByTemplateId(TEMPLATE_ID);
        verify(barcodeTemplateFieldRepository).saveAll(any());
        verify(barcodeTemplateRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("修改模板：已被绑定时保留原行并生成升版本新行")
    void updateBarcodeTemplateCreatesNextVersionWhenBound() {
        BarcodeTemplateEntity existing = buildTemplateEntity("V1");
        when(barcodeTemplateRepository.findByIdAndDeletedFalse(TEMPLATE_ID))
                .thenReturn(Optional.of(existing));
        when(barcodeApplyRuleRepository.existsByTemplateIdAndDeletedFalse(TEMPLATE_ID)).thenReturn(true);
        BarcodeTemplateEntity versionThree = buildTemplateEntity("V3");
        when(barcodeTemplateRepository.findByTemplateCodeAndDeletedFalse("TPL01"))
                .thenReturn(List.of(existing, versionThree));
        when(barcodeTemplateRepository.saveAndFlush(any(BarcodeTemplateEntity.class)))
                .thenAnswer(invocation -> {
                    BarcodeTemplateEntity entity = invocation.getArgument(0);
                    entity.setId(999L);
                    return entity;
                });

        barcodeTemplateService.updateBarcodeTemplate(TEMPLATE_ID, buildSaveReqVO());

        ArgumentCaptor<BarcodeTemplateEntity> captor = ArgumentCaptor.forClass(BarcodeTemplateEntity.class);
        verify(barcodeTemplateRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getTemplateCode()).isEqualTo("TPL01");
        // 既有版本 V1/V3，下一版本应为 V4
        assertThat(captor.getValue().getVersion()).isEqualTo("V4");
        verify(barcodeTemplateRepository, never()).updateInfo(anyLong(), anyString(), any(), any());
        // 原版本字段保持不变，新字段挂在新版本行上
        verify(barcodeTemplateFieldRepository, never()).logicDeleteByTemplateId(TEMPLATE_ID);
        verify(barcodeTemplateFieldRepository).saveAll(any());
    }

    @Test
    @DisplayName("修改模板：模板不存在时报不存在")
    void updateBarcodeTemplateRejectsMissingTemplate() {
        when(barcodeTemplateRepository.findByIdAndDeletedFalse(TEMPLATE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> barcodeTemplateService.updateBarcodeTemplate(TEMPLATE_ID, buildSaveReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_TEMPLATE_NOT_EXISTS));
    }

    @Test
    @DisplayName("启用模板：已启用时报状态错误")
    void enableBarcodeTemplateRejectsAlreadyEnabled() {
        when(barcodeTemplateRepository.updateStatus(TEMPLATE_ID, CommonStatusEnum.DISABLED.getStatus(),
                CommonStatusEnum.ENABLED.getStatus())).thenReturn(0);
        when(barcodeTemplateRepository.findByIdAndDeletedFalse(TEMPLATE_ID))
                .thenReturn(Optional.of(buildTemplateEntity("V1")));

        assertThatThrownBy(() -> barcodeTemplateService.enableBarcodeTemplate(TEMPLATE_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_TEMPLATE_ALREADY_ENABLED));
    }

    @Test
    @DisplayName("停用模板：启用状态 CAS 命中")
    void disableBarcodeTemplateTransitionsStatus() {
        when(barcodeTemplateRepository.updateStatus(TEMPLATE_ID, CommonStatusEnum.ENABLED.getStatus(),
                CommonStatusEnum.DISABLED.getStatus())).thenReturn(1);

        barcodeTemplateService.disableBarcodeTemplate(TEMPLATE_ID);

        verify(barcodeTemplateRepository).updateStatus(TEMPLATE_ID, CommonStatusEnum.ENABLED.getStatus(),
                CommonStatusEnum.DISABLED.getStatus());
    }

    @Test
    @DisplayName("查询详情：返回模板与字段配置")
    void getBarcodeTemplateReturnsFields() {
        when(barcodeTemplateRepository.findByIdAndDeletedFalse(TEMPLATE_ID))
                .thenReturn(Optional.of(buildTemplateEntity("V1")));
        when(barcodeTemplateFieldRepository.findByTemplateIdAndDeletedFalseOrderByIdAsc(TEMPLATE_ID))
                .thenReturn(List.of(buildFieldEntity()));

        BarcodeTemplateRespVO respVO = barcodeTemplateService.getBarcodeTemplate(TEMPLATE_ID);

        assertThat(respVO.getTemplateCode()).isEqualTo("TPL01");
        assertThat(respVO.getFields()).hasSize(1);
    }

    @Test
    @DisplayName("分页查询：总数为 0 时直接返回空页(SQL-005)")
    void getBarcodeTemplatePageShortCircuitsOnZeroTotal() {
        when(barcodeTemplateRepository.count(any(Specification.class))).thenReturn(0L);

        PageResult<BarcodeTemplateRespVO> result =
                barcodeTemplateService.getBarcodeTemplatePage(new BarcodeTemplatePageReqVO());

        assertThat(result.getTotal()).isZero();
        assertThat(result.getList()).isEmpty();
    }

    @Test
    @DisplayName("预览：条码字段取样例条码值，文本字段按数据来源取样例数据，缺样例为 null")
    void previewBarcodeTemplateResolvesSampleContent() {
        when(barcodeTemplateRepository.findByIdAndDeletedFalse(TEMPLATE_ID))
                .thenReturn(Optional.of(buildTemplateEntity("V1")));
        BarcodeTemplateFieldEntity barcodeField = buildFieldEntity();
        BarcodeTemplateFieldEntity textField = new BarcodeTemplateFieldEntity();
        textField.setId(2L);
        textField.setTemplateId(TEMPLATE_ID);
        textField.setFieldName("产品名称");
        textField.setFieldType(BarcodeTemplateFieldTypeEnum.TEXT.getType());
        textField.setDataSource("productName");
        BarcodeTemplateFieldEntity missingField = new BarcodeTemplateFieldEntity();
        missingField.setId(3L);
        missingField.setTemplateId(TEMPLATE_ID);
        missingField.setFieldName("批次号");
        missingField.setFieldType(BarcodeTemplateFieldTypeEnum.TEXT.getType());
        missingField.setDataSource("batchNo");
        when(barcodeTemplateFieldRepository.findByTemplateIdAndDeletedFalseOrderByIdAsc(TEMPLATE_ID))
                .thenReturn(List.of(barcodeField, textField, missingField));

        BarcodeTemplatePreviewReqVO reqVO = new BarcodeTemplatePreviewReqVO();
        reqVO.setTemplateId(TEMPLATE_ID);
        reqVO.setSampleBarcodeValue("YMQ01202607120001");
        reqVO.setSampleData(Map.of("productName", "羽毛球A级"));

        BarcodeTemplatePreviewRespVO respVO = barcodeTemplateService.previewBarcodeTemplate(reqVO);

        assertThat(respVO.getTemplateId()).isEqualTo(TEMPLATE_ID);
        assertThat(respVO.getFields()).hasSize(3);
        assertThat(respVO.getFields().get(0).getSampleContent()).isEqualTo("YMQ01202607120001");
        assertThat(respVO.getFields().get(1).getSampleContent()).isEqualTo("羽毛球A级");
        assertThat(respVO.getFields().get(2).getSampleContent()).isNull();
    }

    /**
     * 构造合法保存请求：一个条码字段 + 一个文本字段。
     *
     * @return 保存请求 VO
     */
    private BarcodeTemplateSaveReqVO buildSaveReqVO() {
        BarcodeTemplateSaveReqVO reqVO = new BarcodeTemplateSaveReqVO();
        reqVO.setTemplateCode("TPL01");
        reqVO.setTemplateName("产品标签");
        reqVO.setPaperWidth(new BigDecimal("60.00"));
        reqVO.setPaperHeight(new BigDecimal("40.00"));
        reqVO.setFields(List.of(
                buildFieldReqVO("条码值", BarcodeTemplateFieldTypeEnum.BARCODE.getType(), "barcodeValue"),
                buildFieldReqVO("产品名称", BarcodeTemplateFieldTypeEnum.TEXT.getType(), "productName")));
        return reqVO;
    }

    /**
     * 构造字段请求。
     *
     * @param fieldName  字段名称
     * @param fieldType  字段类型
     * @param dataSource 数据来源
     * @return 字段请求 VO
     */
    private BarcodeTemplateFieldSaveReqVO buildFieldReqVO(String fieldName, Integer fieldType,
                                                          String dataSource) {
        BarcodeTemplateFieldSaveReqVO field = new BarcodeTemplateFieldSaveReqVO();
        field.setFieldName(fieldName);
        field.setFieldType(fieldType);
        field.setDataSource(dataSource);
        field.setPosX(new BigDecimal("1.00"));
        field.setPosY(new BigDecimal("1.00"));
        return field;
    }

    /**
     * 构造启用状态的模板实体。
     *
     * @param version 版本号
     * @return 模板实体
     */
    private BarcodeTemplateEntity buildTemplateEntity(String version) {
        BarcodeTemplateEntity entity = new BarcodeTemplateEntity();
        entity.setId(TEMPLATE_ID);
        entity.setTemplateCode("TPL01");
        entity.setTemplateName("产品标签");
        entity.setPaperWidth(new BigDecimal("60.00"));
        entity.setPaperHeight(new BigDecimal("40.00"));
        entity.setVersion(version);
        entity.setStatus(CommonStatusEnum.ENABLED.getStatus());
        entity.setDeleted(false);
        return entity;
    }

    /**
     * 构造条码字段实体。
     *
     * @return 字段实体
     */
    private BarcodeTemplateFieldEntity buildFieldEntity() {
        BarcodeTemplateFieldEntity field = new BarcodeTemplateFieldEntity();
        field.setId(1L);
        field.setTemplateId(TEMPLATE_ID);
        field.setFieldName("条码值");
        field.setFieldType(BarcodeTemplateFieldTypeEnum.BARCODE.getType());
        field.setDataSource("barcodeValue");
        return field;
    }
}
