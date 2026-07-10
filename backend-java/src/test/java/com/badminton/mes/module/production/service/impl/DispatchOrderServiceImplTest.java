package com.badminton.mes.module.production.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.DispatchSaveReqVO;
import com.badminton.mes.module.production.controller.vo.DispatchSuggestRespVO;
import com.badminton.mes.module.production.dal.entity.DispatchAdjustLogEntity;
import com.badminton.mes.module.production.dal.entity.DispatchOrderEntity;
import com.badminton.mes.module.production.dal.entity.FactoryCalendarEntity;
import com.badminton.mes.module.production.dal.entity.ProductionLineEntity;
import com.badminton.mes.module.production.dal.entity.ShiftEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.redis.DispatchNoSequence;
import com.badminton.mes.module.production.dal.redis.WorkOrderCache;
import com.badminton.mes.module.production.dal.repository.DispatchAdjustLogRepository;
import com.badminton.mes.module.production.dal.repository.DispatchOrderRepository;
import com.badminton.mes.module.production.dal.repository.FactoryCalendarRepository;
import com.badminton.mes.module.production.dal.repository.ProductionLineRepository;
import com.badminton.mes.module.production.dal.repository.ShiftRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.enums.DispatchAdjustTypeEnum;
import com.badminton.mes.module.production.enums.DispatchStatusEnum;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DispatchOrderServiceImpl} 单元测试。
 *
 * <p>覆盖防超派双保险、产能校验、非工作日拒绝、状态机 CAS、
 * 取消回退数量与下发后调整必填原因等核心分支，依赖全部 Mock。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@ExtendWith(MockitoExtension.class)
class DispatchOrderServiceImplTest {

    private static final Long WORK_ORDER_ID = 1001L;

    private static final Long DISPATCH_ID = 51L;

    private static final Long LINE_ID = 3L;

    private static final Long SHIFT_ID = 1L;

    private static final Long OPERATOR_ID = 9L;

    /** 测试用排产日期：远期周中工作日 */
    private static final LocalDate PLAN_DATE = LocalDate.of(2026, 7, 15);

    @Mock
    private DispatchOrderRepository dispatchOrderRepository;

    @Mock
    private DispatchAdjustLogRepository dispatchAdjustLogRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private ProductionLineRepository productionLineRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private FactoryCalendarRepository factoryCalendarRepository;

    @Mock
    private DispatchNoSequence dispatchNoSequence;

    @Mock
    private WorkOrderCache workOrderCache;

    @InjectMocks
    private DispatchOrderServiceImpl dispatchOrderService;

    @BeforeEach
    void setUpLoginContext() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(OPERATOR_ID);
        SecurityContextHolder.set("unit-test-token", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    private WorkOrderEntity buildReleasedWorkOrder() {
        WorkOrderEntity workOrder = new WorkOrderEntity();
        workOrder.setId(WORK_ORDER_ID);
        workOrder.setWorkshopId(1L);
        workOrder.setOrderStatus(WorkOrderStatusEnum.RELEASED.getStatus());
        workOrder.setPlanQuantity(10000);
        workOrder.setDispatchedQuantity(3000);
        workOrder.setOverRatio(new BigDecimal("5.00"));
        // 交期相对当前日期，避免测试随时间推移失效(建议算法从 max(今日,计划开始) 起算)
        workOrder.setPlanStartTime(LocalDateTime.now().plusDays(1));
        workOrder.setPlanEndTime(LocalDateTime.now().plusDays(10));
        workOrder.setKitStatus(1);
        return workOrder;
    }

    private ProductionLineEntity buildLine() {
        ProductionLineEntity line = new ProductionLineEntity();
        line.setId(LINE_ID);
        line.setLineName("一号成型线");
        line.setWorkshopId(1L);
        line.setStandardCapacity(5000);
        line.setStatus(1);
        return line;
    }

    private ShiftEntity buildShift(Long id, String name) {
        ShiftEntity shift = new ShiftEntity();
        shift.setId(id);
        shift.setShiftName(name);
        if ("夜班".equals(name)) {
            shift.setStartTime(LocalTime.of(20, 0));
            shift.setEndTime(LocalTime.of(8, 0));
        } else {
            shift.setStartTime(LocalTime.of(8, 0));
            shift.setEndTime(LocalTime.of(20, 0));
        }
        shift.setStatus(1);
        return shift;
    }

    private DispatchSaveReqVO buildSaveReq(int quantity) {
        DispatchSaveReqVO reqVO = new DispatchSaveReqVO();
        reqVO.setWorkOrderId(WORK_ORDER_ID);
        reqVO.setLineId(LINE_ID);
        reqVO.setShiftId(SHIFT_ID);
        reqVO.setPlanDate(PLAN_DATE);
        reqVO.setPlanQuantity(quantity);
        reqVO.setPlanStartTime(PLAN_DATE.atTime(8, 0));
        reqVO.setPlanEndTime(PLAN_DATE.atTime(20, 0));
        return reqVO;
    }

    /** 打通创建校验链路的公共桩：工单锁、产线、班次(2 个启用班次→班次产能 2500)、工作日 */
    private void stubCreatePath(WorkOrderEntity workOrder) {
        when(workOrderRepository.findByIdForUpdate(WORK_ORDER_ID)).thenReturn(Optional.of(workOrder));
        lenient().when(productionLineRepository.findByIdAndDeletedFalse(LINE_ID))
                .thenReturn(Optional.of(buildLine()));
        lenient().when(shiftRepository.findByIdAndDeletedFalse(SHIFT_ID))
                .thenReturn(Optional.of(buildShift(SHIFT_ID, "白班")));
        lenient().when(shiftRepository.findByStatusAndDeletedFalseOrderByIdAsc(1))
                .thenReturn(List.of(buildShift(SHIFT_ID, "白班"), buildShift(2L, "夜班")));
        lenient().when(factoryCalendarRepository
                .findByWorkshopIdAndCalendarDateAndDeletedFalse(1L, PLAN_DATE))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("创建派工：校验通过后落库、累加已派数量、写创建日志并清缓存")
    void createDispatchSavesAndIncreasesQuantity() {
        WorkOrderEntity workOrder = buildReleasedWorkOrder();
        stubCreatePath(workOrder);
        when(dispatchOrderRepository.sumPlannedQuantity(LINE_ID, PLAN_DATE, SHIFT_ID,
                DispatchStatusEnum.CANCELLED.getStatus(), null)).thenReturn(500L);
        when(dispatchNoSequence.nextNo()).thenReturn("DP202607150001");
        when(workOrderRepository.increaseDispatchedQuantity(WORK_ORDER_ID, 2000)).thenReturn(1);

        dispatchOrderService.createDispatch(buildSaveReq(2000));

        InOrder inOrder = inOrder(dispatchNoSequence, workOrderRepository);
        inOrder.verify(dispatchNoSequence).nextNo();
        inOrder.verify(workOrderRepository).findByIdForUpdate(WORK_ORDER_ID);

        ArgumentCaptor<DispatchOrderEntity> captor = ArgumentCaptor.forClass(DispatchOrderEntity.class);
        verify(dispatchOrderRepository).save(captor.capture());
        DispatchOrderEntity saved = captor.getValue();
        assertThat(saved.getDispatchNo()).isEqualTo("DP202607150001");
        assertThat(saved.getDispatchStatus()).isEqualTo(DispatchStatusEnum.PENDING_AUDIT.getStatus());
        assertThat(saved.getSuggest()).isZero();
        assertThat(saved.getCreateBy()).isEqualTo(OPERATOR_ID);

        verify(workOrderRepository).increaseDispatchedQuantity(WORK_ORDER_ID, 2000);
        ArgumentCaptor<DispatchAdjustLogEntity> logCaptor =
                ArgumentCaptor.forClass(DispatchAdjustLogEntity.class);
        verify(dispatchAdjustLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getAdjustType())
                .isEqualTo(DispatchAdjustTypeEnum.MANUAL_CREATE.getType());
        verify(workOrderCache).evictAfterCommit(WORK_ORDER_ID);
    }

    @Test
    @DisplayName("创建派工：数量超过剩余可派(上限 FLOOR(10000×1.05)−3000=7500)抛 A0420")
    void createDispatchRejectsQuantityExceed() {
        WorkOrderEntity workOrder = buildReleasedWorkOrder();
        when(workOrderRepository.findByIdForUpdate(WORK_ORDER_ID)).thenReturn(Optional.of(workOrder));

        assertThatThrownBy(() -> dispatchOrderService.createDispatch(buildSaveReq(7501)))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode())
                                .isEqualTo(ProductionErrorCodeConstants.DISPATCH_QUANTITY_EXCEED));
        verify(dispatchOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("创建派工：兜底 UPDATE 影响行数为 0(并发占满剩余量)抛 A0420 回滚")
    void createDispatchRejectsWhenGuardUpdateMisses() {
        WorkOrderEntity workOrder = buildReleasedWorkOrder();
        stubCreatePath(workOrder);
        when(dispatchOrderRepository.sumPlannedQuantity(anyLong(), any(), anyLong(), anyInt(), isNull()))
                .thenReturn(0L);
        when(dispatchNoSequence.nextNo()).thenReturn("DP202607150002");
        when(workOrderRepository.increaseDispatchedQuantity(WORK_ORDER_ID, 2000)).thenReturn(0);

        assertThatThrownBy(() -> dispatchOrderService.createDispatch(buildSaveReq(2000)))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode())
                                .isEqualTo(ProductionErrorCodeConstants.DISPATCH_QUANTITY_EXCEED));
        verify(dispatchAdjustLogRepository, never()).save(any());
        verify(workOrderCache, never()).evictAfterCommit(anyLong());
    }

    @Test
    @DisplayName("创建派工：同线同班次累计超产能(2500/班)抛 A0420")
    void createDispatchRejectsCapacityExceed() {
        WorkOrderEntity workOrder = buildReleasedWorkOrder();
        stubCreatePath(workOrder);
        // 已排 2000 + 本次 1000 > 5000/2=2500
        when(dispatchOrderRepository.sumPlannedQuantity(LINE_ID, PLAN_DATE, SHIFT_ID,
                DispatchStatusEnum.CANCELLED.getStatus(), null)).thenReturn(2000L);

        assertThatThrownBy(() -> dispatchOrderService.createDispatch(buildSaveReq(1000)))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode())
                                .isEqualTo(ProductionErrorCodeConstants.DISPATCH_CAPACITY_EXCEED));
        verify(dispatchOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("创建派工：排产日期为非工作日抛 A0440")
    void createDispatchRejectsNonWorkday() {
        WorkOrderEntity workOrder = buildReleasedWorkOrder();
        stubCreatePath(workOrder);
        FactoryCalendarEntity holiday = new FactoryCalendarEntity();
        holiday.setCalendarDate(PLAN_DATE);
        holiday.setWorkshopId(1L);
        holiday.setWorkday(0);
        when(factoryCalendarRepository.findByWorkshopIdAndCalendarDateAndDeletedFalse(1L, PLAN_DATE))
                .thenReturn(Optional.of(holiday));

        assertThatThrownBy(() -> dispatchOrderService.createDispatch(buildSaveReq(1000)))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode())
                                .isEqualTo(ProductionErrorCodeConstants.DISPATCH_DATE_NOT_WORKDAY));
    }

    @Test
    @DisplayName("创建派工：已创建状态工单不允许派工抛 A0440")
    void createDispatchRejectsCreatedWorkOrder() {
        WorkOrderEntity workOrder = buildReleasedWorkOrder();
        workOrder.setOrderStatus(WorkOrderStatusEnum.CREATED.getStatus());
        when(workOrderRepository.findByIdForUpdate(WORK_ORDER_ID)).thenReturn(Optional.of(workOrder));

        assertThatThrownBy(() -> dispatchOrderService.createDispatch(buildSaveReq(1000)))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode())
                                .isEqualTo(ProductionErrorCodeConstants.DISPATCH_WORK_ORDER_STATUS_INVALID));
    }

    private DispatchOrderEntity buildDispatch(Integer status) {
        DispatchOrderEntity dispatch = new DispatchOrderEntity();
        dispatch.setId(DISPATCH_ID);
        dispatch.setDispatchNo("DP202607150001");
        dispatch.setWorkOrderId(WORK_ORDER_ID);
        dispatch.setLineId(LINE_ID);
        dispatch.setShiftId(SHIFT_ID);
        dispatch.setPlanDate(PLAN_DATE);
        dispatch.setPlanQuantity(2000);
        dispatch.setPlanStartTime(PLAN_DATE.atTime(8, 0));
        dispatch.setPlanEndTime(PLAN_DATE.atTime(20, 0));
        dispatch.setDispatchStatus(status);
        return dispatch;
    }

    @Test
    @DisplayName("审核派工：CAS 命中落审核人，未命中抛 A0440")
    void auditDispatchUsesCas() {
        when(dispatchOrderRepository.findByIdAndDeletedFalse(DISPATCH_ID))
                .thenReturn(Optional.of(buildDispatch(DispatchStatusEnum.PENDING_AUDIT.getStatus())));
        when(dispatchOrderRepository.updateToAudited(eq(DISPATCH_ID),
                eq(DispatchStatusEnum.PENDING_AUDIT.getStatus()),
                eq(DispatchStatusEnum.AUDITED.getStatus()), eq(OPERATOR_ID), any())).thenReturn(1);

        dispatchOrderService.auditDispatch(DISPATCH_ID);
        verify(dispatchAdjustLogRepository).save(any(DispatchAdjustLogEntity.class));

        when(dispatchOrderRepository.updateToAudited(eq(DISPATCH_ID),
                eq(DispatchStatusEnum.PENDING_AUDIT.getStatus()),
                eq(DispatchStatusEnum.AUDITED.getStatus()), eq(OPERATOR_ID), any())).thenReturn(0);
        assertThatThrownBy(() -> dispatchOrderService.auditDispatch(DISPATCH_ID))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode())
                                .isEqualTo(ProductionErrorCodeConstants.DISPATCH_STATUS_NOT_ALLOW_AUDIT));
    }

    @Test
    @DisplayName("下发派工：仅已审核可下发，CAS 未命中抛 A0440")
    void issueDispatchRejectsNotAudited() {
        when(dispatchOrderRepository.findByIdAndDeletedFalse(DISPATCH_ID))
                .thenReturn(Optional.of(buildDispatch(DispatchStatusEnum.PENDING_AUDIT.getStatus())));
        when(dispatchOrderRepository.updateStatus(DISPATCH_ID,
                DispatchStatusEnum.AUDITED.getStatus(), DispatchStatusEnum.ISSUED.getStatus())).thenReturn(0);

        assertThatThrownBy(() -> dispatchOrderService.issueDispatch(DISPATCH_ID))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode())
                                .isEqualTo(ProductionErrorCodeConstants.DISPATCH_STATUS_NOT_ALLOW_ISSUE));
    }

    @Test
    @DisplayName("取消派工：锁工单、CAS 置已取消并回退已派数量")
    void cancelDispatchDecreasesQuantity() {
        DispatchOrderEntity dispatch = buildDispatch(DispatchStatusEnum.ISSUED.getStatus());
        dispatch.setPlanQuantity(2500);
        when(dispatchOrderRepository.findWorkOrderIdById(DISPATCH_ID)).thenReturn(Optional.of(WORK_ORDER_ID));
        when(workOrderRepository.findByIdForUpdate(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildReleasedWorkOrder()));
        when(dispatchOrderRepository.findByIdForUpdate(DISPATCH_ID)).thenReturn(Optional.of(dispatch));
        when(dispatchOrderRepository.updateStatus(DISPATCH_ID,
                DispatchStatusEnum.ISSUED.getStatus(), DispatchStatusEnum.CANCELLED.getStatus())).thenReturn(1);
        when(workOrderRepository.decreaseDispatchedQuantity(WORK_ORDER_ID, 2500)).thenReturn(1);

        dispatchOrderService.cancelDispatch(DISPATCH_ID, "计划变更");

        verify(workOrderRepository).decreaseDispatchedQuantity(WORK_ORDER_ID, 2500);
        ArgumentCaptor<DispatchAdjustLogEntity> logCaptor =
                ArgumentCaptor.forClass(DispatchAdjustLogEntity.class);
        verify(dispatchAdjustLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getAdjustType()).isEqualTo(DispatchAdjustTypeEnum.CANCEL.getType());
        assertThat(logCaptor.getValue().getAdjustReason()).isEqualTo("计划变更");
        verify(workOrderCache).evictAfterCommit(WORK_ORDER_ID);
    }

    @Test
    @DisplayName("取消派工：已完成状态不允许取消抛 A0440")
    void cancelDispatchRejectsFinished() {
        when(dispatchOrderRepository.findWorkOrderIdById(DISPATCH_ID)).thenReturn(Optional.of(WORK_ORDER_ID));
        when(workOrderRepository.findByIdForUpdate(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildReleasedWorkOrder()));
        when(dispatchOrderRepository.findByIdForUpdate(DISPATCH_ID))
                .thenReturn(Optional.of(buildDispatch(DispatchStatusEnum.FINISHED.getStatus())));

        assertThatThrownBy(() -> dispatchOrderService.cancelDispatch(DISPATCH_ID, "计划变更"))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode())
                                .isEqualTo(ProductionErrorCodeConstants.DISPATCH_STATUS_NOT_ALLOW_CANCEL));
        verify(workOrderRepository, never()).decreaseDispatchedQuantity(anyLong(), anyInt());
    }

    @Test
    @DisplayName("修改派工：已下发未填调整原因抛 A0402")
    void updateDispatchRequiresReasonAfterIssue() {
        when(dispatchOrderRepository.findWorkOrderIdById(DISPATCH_ID)).thenReturn(Optional.of(WORK_ORDER_ID));
        when(workOrderRepository.findByIdForUpdate(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildReleasedWorkOrder()));
        when(dispatchOrderRepository.findByIdForUpdate(DISPATCH_ID))
                .thenReturn(Optional.of(buildDispatch(DispatchStatusEnum.ISSUED.getStatus())));

        assertThatThrownBy(() -> dispatchOrderService.updateDispatch(DISPATCH_ID, buildSaveReq(2500)))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode())
                                .isEqualTo(ProductionErrorCodeConstants.DISPATCH_ADJUST_REASON_REQUIRED));
        verify(dispatchOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("修改派工：数量上调走增量校验并记录前后快照")
    void updateDispatchIncreasesDelta() {
        DispatchOrderEntity dispatch = buildDispatch(DispatchStatusEnum.PENDING_AUDIT.getStatus());
        when(dispatchOrderRepository.findWorkOrderIdById(DISPATCH_ID)).thenReturn(Optional.of(WORK_ORDER_ID));
        when(dispatchOrderRepository.findByIdForUpdate(DISPATCH_ID)).thenReturn(Optional.of(dispatch));
        stubCreatePath(buildReleasedWorkOrder());
        when(dispatchOrderRepository.sumPlannedQuantity(LINE_ID, PLAN_DATE, SHIFT_ID,
                DispatchStatusEnum.CANCELLED.getStatus(), DISPATCH_ID)).thenReturn(0L);
        when(workOrderRepository.increaseDispatchedQuantity(WORK_ORDER_ID, 500)).thenReturn(1);

        dispatchOrderService.updateDispatch(DISPATCH_ID, buildSaveReq(2500));

        verify(workOrderRepository).increaseDispatchedQuantity(WORK_ORDER_ID, 500);
        verify(workOrderCache).evictAfterCommit(WORK_ORDER_ID);
        ArgumentCaptor<DispatchAdjustLogEntity> logCaptor =
                ArgumentCaptor.forClass(DispatchAdjustLogEntity.class);
        verify(dispatchAdjustLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getAdjustType()).isEqualTo(DispatchAdjustTypeEnum.ADJUST.getType());
        assertThat(logCaptor.getValue().getBeforeSnapshot()).contains("\"planQuantity\":2000");
        assertThat(logCaptor.getValue().getAfterSnapshot()).contains("\"planQuantity\":2500");
    }

    @Test
    @DisplayName("修改派工：锁内重读到已取消状态时不允许修改")
    void updateDispatchRejectsCancelledAfterLockedReload() {
        when(dispatchOrderRepository.findWorkOrderIdById(DISPATCH_ID)).thenReturn(Optional.of(WORK_ORDER_ID));
        when(workOrderRepository.findByIdForUpdate(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildReleasedWorkOrder()));
        when(dispatchOrderRepository.findByIdForUpdate(DISPATCH_ID))
                .thenReturn(Optional.of(buildDispatch(DispatchStatusEnum.CANCELLED.getStatus())));

        assertThatThrownBy(() -> dispatchOrderService.updateDispatch(DISPATCH_ID, buildSaveReq(2500)))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode())
                                .isEqualTo(ProductionErrorCodeConstants.DISPATCH_STATUS_NOT_ALLOW_UPDATE));
        verify(dispatchOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("修改派工：工单已完工时拒绝调整存量派工")
    void updateDispatchRejectsFinishedWorkOrder() {
        WorkOrderEntity workOrder = buildReleasedWorkOrder();
        workOrder.setOrderStatus(WorkOrderStatusEnum.FINISHED.getStatus());
        when(dispatchOrderRepository.findWorkOrderIdById(DISPATCH_ID)).thenReturn(Optional.of(WORK_ORDER_ID));
        when(workOrderRepository.findByIdForUpdate(WORK_ORDER_ID)).thenReturn(Optional.of(workOrder));

        assertThatThrownBy(() -> dispatchOrderService.updateDispatch(DISPATCH_ID, buildSaveReq(2500)))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode())
                                .isEqualTo(ProductionErrorCodeConstants.DISPATCH_WORK_ORDER_STATUS_INVALID));
        verify(dispatchOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("修改派工：缩量回退影响行数为 0 时抛 B0001")
    void updateDispatchRejectsDecreaseGuardMiss() {
        DispatchOrderEntity dispatch = buildDispatch(DispatchStatusEnum.PENDING_AUDIT.getStatus());
        when(dispatchOrderRepository.findWorkOrderIdById(DISPATCH_ID)).thenReturn(Optional.of(WORK_ORDER_ID));
        when(dispatchOrderRepository.findByIdForUpdate(DISPATCH_ID)).thenReturn(Optional.of(dispatch));
        stubCreatePath(buildReleasedWorkOrder());
        when(dispatchOrderRepository.sumPlannedQuantity(LINE_ID, PLAN_DATE, SHIFT_ID,
                DispatchStatusEnum.CANCELLED.getStatus(), DISPATCH_ID)).thenReturn(0L);
        when(workOrderRepository.decreaseDispatchedQuantity(WORK_ORDER_ID, 500)).thenReturn(0);

        assertThatThrownBy(() -> dispatchOrderService.updateDispatch(DISPATCH_ID, buildSaveReq(1500)))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCodeConstants.SYSTEM_ERROR));
    }

    @Test
    @DisplayName("取消派工：回退影响行数为 0 时抛 B0001")
    void cancelDispatchRejectsDecreaseGuardMiss() {
        DispatchOrderEntity dispatch = buildDispatch(DispatchStatusEnum.ISSUED.getStatus());
        when(dispatchOrderRepository.findWorkOrderIdById(DISPATCH_ID)).thenReturn(Optional.of(WORK_ORDER_ID));
        when(workOrderRepository.findByIdForUpdate(WORK_ORDER_ID))
                .thenReturn(Optional.of(buildReleasedWorkOrder()));
        when(dispatchOrderRepository.findByIdForUpdate(DISPATCH_ID)).thenReturn(Optional.of(dispatch));
        when(dispatchOrderRepository.updateStatus(DISPATCH_ID,
                DispatchStatusEnum.ISSUED.getStatus(), DispatchStatusEnum.CANCELLED.getStatus())).thenReturn(1);
        when(workOrderRepository.decreaseDispatchedQuantity(WORK_ORDER_ID, 2000)).thenReturn(0);

        assertThatThrownBy(() -> dispatchOrderService.cancelDispatch(DISPATCH_ID, "计划变更"))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCodeConstants.SYSTEM_ERROR));
    }

    @Test
    @DisplayName("排产建议：按剩余产能贪心填充，覆盖剩余可派 7500")
    void suggestDispatchFillsGreedy() {
        WorkOrderEntity workOrder = buildReleasedWorkOrder();
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID)).thenReturn(Optional.of(workOrder));
        when(productionLineRepository.findByWorkshopIdAndStatusAndDeletedFalseOrderByIdAsc(1L, 1))
                .thenReturn(List.of(buildLine()));
        when(shiftRepository.findByStatusAndDeletedFalseOrderByIdAsc(1))
                .thenReturn(List.of(buildShift(SHIFT_ID, "白班"), buildShift(2L, "夜班")));
        when(factoryCalendarRepository.findByWorkshopIdAndCalendarDateBetweenAndDeletedFalse(
                eq(1L), any(), any())).thenReturn(List.of());
        // 每格(班次)产能 5000/2=2500，全部空闲
        when(dispatchOrderRepository.sumPlannedQuantity(eq(LINE_ID), any(), anyLong(),
                eq(DispatchStatusEnum.CANCELLED.getStatus()), isNull())).thenReturn(0L);

        List<DispatchSuggestRespVO> suggestions = dispatchOrderService.suggestDispatch(WORK_ORDER_ID);

        // 剩余可派 7500 = 2500 + 2500 + 2500，三格填满
        assertThat(suggestions).hasSize(3);
        assertThat(suggestions.stream().mapToInt(DispatchSuggestRespVO::getPlanQuantity).sum())
                .isEqualTo(7500);
        assertThat(suggestions.get(0).getPlanQuantity()).isEqualTo(2500);
        assertThat(suggestions.get(0).getKitStatus()).isEqualTo(1);
        assertThat(suggestions).allSatisfy(suggestion -> assertThat(suggestion.getCanFinishOnTime()).isTrue());
        assertThat(suggestions.get(0).getPlanStartTime()).isEqualTo(suggestions.get(0).getPlanDate().atTime(8, 0));
        assertThat(suggestions.get(0).getPlanEndTime()).isEqualTo(suggestions.get(0).getPlanDate().atTime(20, 0));
        assertThat(suggestions.get(1).getPlanStartTime()).isEqualTo(suggestions.get(1).getPlanDate().atTime(20, 0));
        assertThat(suggestions.get(1).getPlanEndTime())
                .isEqualTo(suggestions.get(1).getPlanDate().plusDays(1).atTime(8, 0));
        // 建议按日期升序：首格日期不晚于次格
        assertThat(suggestions.get(0).getPlanDate()).isBeforeOrEqualTo(suggestions.get(2).getPlanDate());
    }

    @Test
    @DisplayName("排产建议：格子用尽仍有缺口时标记不能按期排完")
    void suggestDispatchMarksCannotFinishOnTime() {
        WorkOrderEntity workOrder = buildReleasedWorkOrder();
        LocalDate oneDay = LocalDate.now().plusDays(1);
        workOrder.setPlanStartTime(oneDay.atTime(8, 0));
        workOrder.setPlanEndTime(oneDay.atTime(20, 0));
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID)).thenReturn(Optional.of(workOrder));
        when(productionLineRepository.findByWorkshopIdAndStatusAndDeletedFalseOrderByIdAsc(1L, 1))
                .thenReturn(List.of(buildLine()));
        when(shiftRepository.findByStatusAndDeletedFalseOrderByIdAsc(1))
                .thenReturn(List.of(buildShift(SHIFT_ID, "白班"), buildShift(2L, "夜班")));
        when(factoryCalendarRepository.findByWorkshopIdAndCalendarDateBetweenAndDeletedFalse(
                eq(1L), any(), any())).thenReturn(List.of());
        when(dispatchOrderRepository.sumPlannedQuantity(eq(LINE_ID), any(), anyLong(),
                eq(DispatchStatusEnum.CANCELLED.getStatus()), isNull())).thenReturn(0L);

        List<DispatchSuggestRespVO> suggestions = dispatchOrderService.suggestDispatch(WORK_ORDER_ID);

        assertThat(suggestions).hasSize(2);
        assertThat(suggestions.stream().mapToInt(DispatchSuggestRespVO::getPlanQuantity).sum())
                .isEqualTo(5000);
        assertThat(suggestions).allSatisfy(suggestion -> assertThat(suggestion.getCanFinishOnTime()).isFalse());
    }

    @Test
    @DisplayName("排产建议：已派满(剩余 0)返回空集合")
    void suggestDispatchReturnsEmptyWhenFullyDispatched() {
        WorkOrderEntity workOrder = buildReleasedWorkOrder();
        workOrder.setDispatchedQuantity(10500);
        when(workOrderRepository.findByIdAndDeletedFalse(WORK_ORDER_ID)).thenReturn(Optional.of(workOrder));

        assertThat(dispatchOrderService.suggestDispatch(WORK_ORDER_ID)).isEmpty();
    }
}
