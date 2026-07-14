package com.badminton.mes.module.craft.service;

import java.util.List;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.craft.constants.CraftErrorCodeConstants;
import com.badminton.mes.module.craft.controller.vo.CraftRouteStepSaveReqVO;
import com.badminton.mes.module.craft.dal.entity.CraftProcessEntity;
import com.badminton.mes.module.craft.dal.entity.CraftProcessSopEntity;
import com.badminton.mes.module.craft.dal.entity.CraftQualityPlanReferenceEntity;
import com.badminton.mes.module.craft.dal.entity.CraftRouteDetailEntity;
import com.badminton.mes.module.craft.dal.entity.CraftRouteProductEntity;
import com.badminton.mes.module.craft.dal.entity.CraftWorkstationReferenceEntity;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.craft.dal.repository.CraftProcessSopRepository;
import com.badminton.mes.module.craft.dal.repository.CraftQualityPlanReferenceRepository;
import com.badminton.mes.module.craft.dal.repository.CraftWorkstationReferenceRepository;
import com.badminton.mes.module.equipment.dal.entity.EquipmentCategoryEntity;
import com.badminton.mes.module.equipment.dal.repository.EquipmentCategoryRepository;
import com.badminton.mes.module.production.dal.entity.ProductEntity;
import com.badminton.mes.module.production.dal.repository.ProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CraftRouteReferenceValidator} 单元测试。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@ExtendWith(MockitoExtension.class)
class CraftRouteReferenceValidatorTest {

    private static final Long PRODUCT_ID = 10L;

    private static final Long PROCESS_ID = 20L;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CraftProcessRepository processRepository;

    @Mock
    private CraftWorkstationReferenceRepository workstationRepository;

    @Mock
    private EquipmentCategoryRepository equipmentCategoryRepository;

    @Mock
    private CraftProcessSopRepository sopRepository;

    @Mock
    private CraftQualityPlanReferenceRepository qualityPlanRepository;

    private CraftRouteReferenceValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CraftRouteReferenceValidator(productRepository, processRepository,
                workstationRepository, equipmentCategoryRepository, sopRepository, qualityPlanRepository);
        when(productRepository.findByIdInAndStatusAndDeletedFalse(any(), eq(1)))
                .thenReturn(List.of(buildProduct()));
    }

    @Test
    @DisplayName("保存草稿：工序质检默认规则继承到实际待落库步骤")
    void saveInheritsQualityRulesIntoPersistentRequest() {
        CraftProcessEntity process = buildProcess();
        process.setQualityRequired(true);
        process.setQualityPlanId(60L);
        when(processRepository.findByIdInAndStatusAndDeletedFalse(any(), eq(1)))
                .thenReturn(List.of(process));
        CraftQualityPlanReferenceEntity plan = new CraftQualityPlanReferenceEntity();
        plan.setId(60L);
        when(qualityPlanRepository.findByIdInAndStatusAndDeletedFalse(any(), eq(1)))
                .thenReturn(List.of(plan));
        CraftRouteStepSaveReqVO step = buildStep();

        validator.validateForSave(List.of(PRODUCT_ID), List.of(step));

        assertThat(step.getInspectNode()).isTrue();
        assertThat(step.getQualityPlanId()).isEqualTo(60L);
    }

    @Test
    @DisplayName("审核路线：工序规则变化后不修改临时步骤，严格拒绝落库配置不一致")
    void approvalRejectsChangedQualityRuleWithoutMutatingDetail() {
        CraftProcessEntity process = buildProcess();
        process.setQualityRequired(true);
        process.setQualityPlanId(60L);
        when(processRepository.findAvailableByIdInForUpdateOrderByIdAsc(any(), eq(1)))
                .thenReturn(List.of(process));
        CraftRouteDetailEntity detail = buildDetail();

        assertThatThrownBy(() -> validator.validateForApproval(
                List.of(buildRelation()), List.of(detail)))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isSameAs(CraftErrorCodeConstants.ROUTE_CONFIGURATION_INCOMPLETE));
        assertThat(detail.getInspect()).isFalse();
        assertThat(detail.getQualityPlanId()).isNull();
    }

    @Test
    @DisplayName("审核路线：按固定引用类型使用写锁查询并完成完整性复核")
    void approvalLocksEveryReferencedMasterData() {
        when(processRepository.findAvailableByIdInForUpdateOrderByIdAsc(any(), eq(1)))
                .thenReturn(List.of(buildProcess()));
        CraftWorkstationReferenceEntity station = new CraftWorkstationReferenceEntity();
        station.setId(30L);
        when(workstationRepository.findAvailableByIdInForUpdateOrderByIdAsc(any(), eq(1)))
                .thenReturn(List.of(station));
        EquipmentCategoryEntity category = new EquipmentCategoryEntity();
        category.setId(40L);
        when(equipmentCategoryRepository.findAvailableByIdInForUpdateOrderByIdAsc(any(), eq(1)))
                .thenReturn(List.of(category));
        CraftProcessSopEntity sop = new CraftProcessSopEntity();
        sop.setId(50L);
        sop.setProcessId(PROCESS_ID);
        when(sopRepository.findAvailableByIdInForUpdateOrderByIdAsc(any(), eq(1)))
                .thenReturn(List.of(sop));
        CraftQualityPlanReferenceEntity plan = new CraftQualityPlanReferenceEntity();
        plan.setId(60L);
        when(qualityPlanRepository.findAvailableByIdInForUpdateOrderByIdAsc(any(), eq(1)))
                .thenReturn(List.of(plan));
        CraftRouteDetailEntity detail = buildDetail();
        detail.setStationId(30L);
        detail.setEquipmentCategoryId(40L);
        detail.setInspect(true);
        detail.setSopId(50L);
        detail.setQualityPlanId(60L);

        assertThatCode(() -> validator.validateForApproval(
                List.of(buildRelation()), List.of(detail))).doesNotThrowAnyException();

        verify(processRepository).findAvailableByIdInForUpdateOrderByIdAsc(any(), eq(1));
        verify(workstationRepository).findAvailableByIdInForUpdateOrderByIdAsc(any(), eq(1));
        verify(equipmentCategoryRepository).findAvailableByIdInForUpdateOrderByIdAsc(any(), eq(1));
        verify(sopRepository).findAvailableByIdInForUpdateOrderByIdAsc(any(), eq(1));
        verify(qualityPlanRepository).findAvailableByIdInForUpdateOrderByIdAsc(any(), eq(1));
    }

    /** 构造启用产品。 */
    private ProductEntity buildProduct() {
        ProductEntity product = new ProductEntity();
        product.setId(PRODUCT_ID);
        return product;
    }

    /** 构造无质检要求的工序。 */
    private CraftProcessEntity buildProcess() {
        CraftProcessEntity process = new CraftProcessEntity();
        process.setId(PROCESS_ID);
        process.setKeyProcess(false);
        process.setQualityRequired(false);
        return process;
    }

    /** 构造草稿步骤请求。 */
    private CraftRouteStepSaveReqVO buildStep() {
        CraftRouteStepSaveReqVO step = new CraftRouteStepSaveReqVO();
        step.setSequenceNo(1);
        step.setProcessId(PROCESS_ID);
        step.setInspectNode(false);
        return step;
    }

    /** 构造路线产品关系。 */
    private CraftRouteProductEntity buildRelation() {
        CraftRouteProductEntity relation = new CraftRouteProductEntity();
        relation.setProductId(PRODUCT_ID);
        return relation;
    }

    /** 构造持久化路线明细。 */
    private CraftRouteDetailEntity buildDetail() {
        CraftRouteDetailEntity detail = new CraftRouteDetailEntity();
        detail.setSequenceNo(1);
        detail.setProcessId(PROCESS_ID);
        detail.setInspect(false);
        return detail;
    }
}
