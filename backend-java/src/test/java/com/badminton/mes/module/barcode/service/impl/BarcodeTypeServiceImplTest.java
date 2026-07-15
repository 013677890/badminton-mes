package com.badminton.mes.module.barcode.service.impl;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.barcode.constants.BarcodeErrorCodeConstants;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTypePageReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTypeRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTypeSaveReqVO;
import com.badminton.mes.module.barcode.dal.entity.BarcodeTypeEntity;
import com.badminton.mes.module.barcode.dal.repository.BarcodeApplyRuleRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeRuleRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeTypeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link BarcodeTypeServiceImpl} 单元测试。
 *
 * <p>数据库依赖全部 Mock，不依赖外部环境，可重复执行。覆盖正常路径与
 * 编码重复、非法状态、引用占用、并发兜底等异常路径。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@ExtendWith(MockitoExtension.class)
class BarcodeTypeServiceImplTest {

    /** 测试用类型 id */
    private static final Long TYPE_ID = 100L;

    @Mock
    private BarcodeTypeRepository barcodeTypeRepository;

    @Mock
    private BarcodeRuleRepository barcodeRuleRepository;

    @Mock
    private BarcodeApplyRuleRepository barcodeApplyRuleRepository;

    private BarcodeTypeServiceImpl barcodeTypeService;

    @BeforeEach
    void setUp() {
        barcodeTypeService = new BarcodeTypeServiceImpl(barcodeTypeRepository, barcodeRuleRepository,
                barcodeApplyRuleRepository);
    }

    @Test
    @DisplayName("创建类型：编码不重复时落库并默认启用")
    void createBarcodeTypeSavesEnabledEntity() {
        BarcodeTypeSaveReqVO reqVO = buildSaveReqVO();
        when(barcodeTypeRepository.existsByTypeCodeAndDeletedFalse("PRODUCT")).thenReturn(false);
        when(barcodeTypeRepository.saveAndFlush(any(BarcodeTypeEntity.class))).thenAnswer(invocation -> {
            BarcodeTypeEntity entity = invocation.getArgument(0);
            entity.setId(TYPE_ID);
            return entity;
        });

        Long id = barcodeTypeService.createBarcodeType(reqVO);

        assertThat(id).isEqualTo(TYPE_ID);
        ArgumentCaptor<BarcodeTypeEntity> captor = ArgumentCaptor.forClass(BarcodeTypeEntity.class);
        verify(barcodeTypeRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getTypeCode()).isEqualTo("PRODUCT");
        assertThat(captor.getValue().getTypeName()).isEqualTo("产品码");
        assertThat(captor.getValue().getStatus()).isEqualTo(CommonStatusEnum.ENABLED.getStatus());
    }

    @Test
    @DisplayName("创建类型：编码已存在时报重复且不落库")
    void createBarcodeTypeRejectsDuplicateCode() {
        when(barcodeTypeRepository.existsByTypeCodeAndDeletedFalse("PRODUCT")).thenReturn(true);

        assertThatThrownBy(() -> barcodeTypeService.createBarcodeType(buildSaveReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_TYPE_CODE_DUPLICATE));
        verify(barcodeTypeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("创建类型：并发穿透查重时由唯一索引兜底转业务错误")
    void createBarcodeTypeTranslatesUniqueViolation() {
        when(barcodeTypeRepository.existsByTypeCodeAndDeletedFalse("PRODUCT")).thenReturn(false);
        when(barcodeTypeRepository.saveAndFlush(any(BarcodeTypeEntity.class)))
                .thenThrow(new DataIntegrityViolationException("uk_type_code"));

        assertThatThrownBy(() -> barcodeTypeService.createBarcodeType(buildSaveReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_TYPE_CODE_DUPLICATE));
    }

    @Test
    @DisplayName("修改类型：合法请求执行 CAS 更新")
    void updateBarcodeTypeUpdatesInfo() {
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(buildEnabledEntity()));
        when(barcodeTypeRepository.existsByTypeCodeAndIdNotAndDeletedFalse("PRODUCT", TYPE_ID))
                .thenReturn(false);
        when(barcodeTypeRepository.updateInfo(TYPE_ID, "PRODUCT", "产品码", "成品")).thenReturn(1);

        barcodeTypeService.updateBarcodeType(TYPE_ID, buildSaveReqVO());

        verify(barcodeTypeRepository).updateInfo(TYPE_ID, "PRODUCT", "产品码", "成品");
    }

    @Test
    @DisplayName("修改类型：类型不存在时报不存在")
    void updateBarcodeTypeRejectsMissingType() {
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> barcodeTypeService.updateBarcodeType(TYPE_ID, buildSaveReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_TYPE_NOT_EXISTS));
    }

    @Test
    @DisplayName("修改类型：编码被其他类型占用时报重复")
    void updateBarcodeTypeRejectsDuplicateCode() {
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(buildEnabledEntity()));
        when(barcodeTypeRepository.existsByTypeCodeAndIdNotAndDeletedFalse("PRODUCT", TYPE_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> barcodeTypeService.updateBarcodeType(TYPE_ID, buildSaveReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_TYPE_CODE_DUPLICATE));
        verify(barcodeTypeRepository, never()).updateInfo(any(), any(), any(), any());
    }

    @Test
    @DisplayName("修改类型：CAS 未命中(并发删除)时报不存在")
    void updateBarcodeTypeRejectsConcurrentDelete() {
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(buildEnabledEntity()));
        when(barcodeTypeRepository.existsByTypeCodeAndIdNotAndDeletedFalse("PRODUCT", TYPE_ID))
                .thenReturn(false);
        when(barcodeTypeRepository.updateInfo(TYPE_ID, "PRODUCT", "产品码", "成品")).thenReturn(0);

        assertThatThrownBy(() -> barcodeTypeService.updateBarcodeType(TYPE_ID, buildSaveReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_TYPE_NOT_EXISTS));
    }

    @Test
    @DisplayName("启用类型：停用状态 CAS 命中")
    void enableBarcodeTypeTransitionsStatus() {
        when(barcodeTypeRepository.updateStatus(TYPE_ID, CommonStatusEnum.DISABLED.getStatus(),
                CommonStatusEnum.ENABLED.getStatus())).thenReturn(1);

        barcodeTypeService.enableBarcodeType(TYPE_ID);

        verify(barcodeTypeRepository).updateStatus(TYPE_ID, CommonStatusEnum.DISABLED.getStatus(),
                CommonStatusEnum.ENABLED.getStatus());
    }

    @Test
    @DisplayName("启用类型：已启用时报状态错误")
    void enableBarcodeTypeRejectsAlreadyEnabled() {
        when(barcodeTypeRepository.updateStatus(TYPE_ID, CommonStatusEnum.DISABLED.getStatus(),
                CommonStatusEnum.ENABLED.getStatus())).thenReturn(0);
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(buildEnabledEntity()));

        assertThatThrownBy(() -> barcodeTypeService.enableBarcodeType(TYPE_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_TYPE_ALREADY_ENABLED));
    }

    @Test
    @DisplayName("启用类型：类型不存在时报不存在而非状态错误")
    void enableBarcodeTypeRejectsMissingType() {
        when(barcodeTypeRepository.updateStatus(TYPE_ID, CommonStatusEnum.DISABLED.getStatus(),
                CommonStatusEnum.ENABLED.getStatus())).thenReturn(0);
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> barcodeTypeService.enableBarcodeType(TYPE_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_TYPE_NOT_EXISTS));
    }

    @Test
    @DisplayName("停用类型：已停用时报状态错误")
    void disableBarcodeTypeRejectsAlreadyDisabled() {
        when(barcodeTypeRepository.updateStatus(TYPE_ID, CommonStatusEnum.ENABLED.getStatus(),
                CommonStatusEnum.DISABLED.getStatus())).thenReturn(0);
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(buildEnabledEntity()));

        assertThatThrownBy(() -> barcodeTypeService.disableBarcodeType(TYPE_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_TYPE_ALREADY_DISABLED));
    }

    @Test
    @DisplayName("删除类型：被条码规则引用时拒绝删除")
    void deleteBarcodeTypeRejectsRuleReference() {
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(buildEnabledEntity()));
        when(barcodeRuleRepository.existsByBarcodeTypeIdAndDeletedFalse(TYPE_ID)).thenReturn(true);

        assertThatThrownBy(() -> barcodeTypeService.deleteBarcodeType(TYPE_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_TYPE_IN_USE_NOT_DELETE));
        verify(barcodeTypeRepository, never()).logicDeleteById(any());
    }

    @Test
    @DisplayName("删除类型：被应用规则引用时拒绝删除")
    void deleteBarcodeTypeRejectsApplyRuleReference() {
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(buildEnabledEntity()));
        when(barcodeRuleRepository.existsByBarcodeTypeIdAndDeletedFalse(TYPE_ID)).thenReturn(false);
        when(barcodeApplyRuleRepository.existsByBarcodeTypeIdAndDeletedFalse(TYPE_ID)).thenReturn(true);

        assertThatThrownBy(() -> barcodeTypeService.deleteBarcodeType(TYPE_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_TYPE_IN_USE_NOT_DELETE));
        verify(barcodeTypeRepository, never()).logicDeleteById(any());
    }

    @Test
    @DisplayName("删除类型：无引用时逻辑删除")
    void deleteBarcodeTypeLogicallyDeletes() {
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID))
                .thenReturn(Optional.of(buildEnabledEntity()));
        when(barcodeRuleRepository.existsByBarcodeTypeIdAndDeletedFalse(TYPE_ID)).thenReturn(false);
        when(barcodeApplyRuleRepository.existsByBarcodeTypeIdAndDeletedFalse(TYPE_ID)).thenReturn(false);
        when(barcodeTypeRepository.logicDeleteById(TYPE_ID)).thenReturn(1);

        barcodeTypeService.deleteBarcodeType(TYPE_ID);

        verify(barcodeTypeRepository).logicDeleteById(TYPE_ID);
    }

    @Test
    @DisplayName("查询详情：类型不存在时报不存在")
    void getBarcodeTypeRejectsMissingType() {
        when(barcodeTypeRepository.findByIdAndDeletedFalse(TYPE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> barcodeTypeService.getBarcodeType(TYPE_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(BarcodeErrorCodeConstants.BARCODE_TYPE_NOT_EXISTS));
    }

    @Test
    @DisplayName("分页查询：总数为 0 时直接返回空页，不执行列表查询(SQL-005)")
    void getBarcodeTypePageShortCircuitsOnZeroTotal() {
        when(barcodeTypeRepository.count(any(Specification.class))).thenReturn(0L);

        BarcodeTypePageReqVO reqVO = new BarcodeTypePageReqVO();
        PageResult<BarcodeTypeRespVO> result = barcodeTypeService.getBarcodeTypePage(reqVO);

        assertThat(result.getTotal()).isZero();
        assertThat(result.getList()).isEmpty();
        verify(barcodeTypeRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("分页查询：请求页码超过总页数时按最后一页返回(API-009)")
    void getBarcodeTypePageClampsOverflowPageNo() {
        when(barcodeTypeRepository.count(any(Specification.class))).thenReturn(3L);
        when(barcodeTypeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildEnabledEntity())));

        BarcodeTypePageReqVO reqVO = new BarcodeTypePageReqVO();
        reqVO.setPageNo(99);
        reqVO.setPageSize(10);
        PageResult<BarcodeTypeRespVO> result = barcodeTypeService.getBarcodeTypePage(reqVO);

        assertThat(result.getPageNo()).isEqualTo(1);
        assertThat(result.getTotal()).isEqualTo(3L);
        assertThat(result.getList()).hasSize(1);
    }

    @Test
    @DisplayName("启用选项：仅返回启用状态类型")
    void getEnabledBarcodeTypeOptionsReturnsEnabledOnly() {
        when(barcodeTypeRepository.findByStatusAndDeletedFalseOrderByTypeCodeAsc(
                CommonStatusEnum.ENABLED.getStatus())).thenReturn(List.of(buildEnabledEntity()));

        List<BarcodeTypeRespVO> options = barcodeTypeService.getEnabledBarcodeTypeOptions();

        assertThat(options).hasSize(1);
        assertThat(options.get(0).getTypeCode()).isEqualTo("PRODUCT");
    }

    /**
     * 构造合法保存请求。
     *
     * @return 保存请求 VO
     */
    private BarcodeTypeSaveReqVO buildSaveReqVO() {
        BarcodeTypeSaveReqVO reqVO = new BarcodeTypeSaveReqVO();
        reqVO.setTypeCode("PRODUCT");
        reqVO.setTypeName("产品码");
        reqVO.setApplyObject("成品");
        return reqVO;
    }

    /**
     * 构造启用状态的类型实体。
     *
     * @return 类型实体
     */
    private BarcodeTypeEntity buildEnabledEntity() {
        BarcodeTypeEntity entity = new BarcodeTypeEntity();
        entity.setId(TYPE_ID);
        entity.setTypeCode("PRODUCT");
        entity.setTypeName("产品码");
        entity.setStatus(CommonStatusEnum.ENABLED.getStatus());
        entity.setDeleted(false);
        return entity;
    }
}
