package com.badminton.mes.module.scene.service;

import java.math.BigDecimal;
import java.util.List;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.dal.entity.CraftRouteDetailEntity;
import com.badminton.mes.module.craft.dal.repository.CraftRouteDetailRepository;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.dal.entity.DispatchOrderEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.scene.dal.entity.SceneProcessTaskEntity;
import com.badminton.mes.module.scene.dal.entity.SceneProductionTaskEntity;
import com.badminton.mes.module.scene.dal.repository.SceneDependencyQueryRepository;
import com.badminton.mes.module.scene.dal.repository.SceneDependencyQueryRepository.WorkOrderSnapshot;
import com.badminton.mes.module.scene.dal.repository.SceneProcessTaskRepository;
import com.badminton.mes.module.scene.dal.repository.SceneProductionTaskRepository;
import com.badminton.mes.module.scene.enums.SceneTaskStatusEnum;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 派工下发到现场任务的事务服务。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Service
public class SceneTaskService {

    /** prod_task.source_type 取值：1 派工下发。 */
    private static final int SOURCE_TYPE_DISPATCH_ISSUE = 1;

    private final SceneProductionTaskRepository productionTaskRepository;
    private final SceneProcessTaskRepository processTaskRepository;
    private final WorkOrderRepository workOrderRepository;
    private final CraftRouteDetailRepository routeDetailRepository;
    private final SceneDependencyQueryRepository dependencyQueryRepository;

    public SceneTaskService(SceneProductionTaskRepository productionTaskRepository,
                            SceneProcessTaskRepository processTaskRepository,
                            WorkOrderRepository workOrderRepository,
                            CraftRouteDetailRepository routeDetailRepository,
                            SceneDependencyQueryRepository dependencyQueryRepository) {
        this.productionTaskRepository = productionTaskRepository;
        this.processTaskRepository = processTaskRepository;
        this.workOrderRepository = workOrderRepository;
        this.routeDetailRepository = routeDetailRepository;
        this.dependencyQueryRepository = dependencyQueryRepository;
    }

    /**
     * 幂等生成现场生产任务及工序任务快照。
     *
     * <p>prod_task 的工单、产品、路线、车间产线快照列均为非空列，
     * 统一从 {@link SceneDependencyQueryRepository} 的联查快照取值，缺一不可。
     *
     * @param dispatch 已审核派工单
     * @return 现场生产任务主键
     */
    @Transactional(rollbackFor = Exception.class)
    public Long issueDispatch(DispatchOrderEntity dispatch) {
        var existing = productionTaskRepository
                .findByDispatchOrderIdAndDeletedFalse(dispatch.getId());
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        WorkOrderEntity workOrder = workOrderRepository
                .findByIdAndDeletedFalse(dispatch.getWorkOrderId())
                .orElseThrow(() -> new ServiceException(
                        ProductionErrorCodeConstants.WORK_ORDER_NOT_EXISTS));
        if (workOrder.getRoutingId() == null) {
            throw new ServiceException(
                    ProductionErrorCodeConstants.WORK_ORDER_RELEASE_MISSING_BOM_ROUTING);
        }

        WorkOrderSnapshot snapshot = dependencyQueryRepository
                .findWorkOrderSnapshot(dispatch.getWorkOrderId(), dispatch.getLineId())
                .orElseThrow(() -> new ServiceException(
                        ProductionErrorCodeConstants.WORK_ORDER_ROUTING_NOT_AVAILABLE));

        List<CraftRouteDetailEntity> routeSteps = routeDetailRepository
                .findByRouteIdAndDeletedFalseOrderBySequenceNoAsc(workOrder.getRoutingId());
        if (routeSteps.isEmpty()) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_ROUTING_NOT_AVAILABLE);
        }

        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        SceneProductionTaskEntity task = new SceneProductionTaskEntity();
        task.setTaskNo(dispatch.getDispatchNo());
        task.setSourceType(SOURCE_TYPE_DISPATCH_ISSUE);
        task.setDispatchOrderId(dispatch.getId());
        task.setWorkOrderId(dispatch.getWorkOrderId());
        task.setWorkOrderNo(snapshot.workOrderNo());
        task.setProductId(snapshot.productId());
        task.setProductCode(snapshot.productCode());
        task.setProductName(snapshot.productName());
        task.setBatchNo(snapshot.batchNo());
        task.setRoutingId(workOrder.getRoutingId());
        task.setRoutingCode(snapshot.routingCode());
        task.setRoutingVersion(snapshot.routingVersion());
        task.setWorkshopId(snapshot.workshopId());
        task.setWorkshopName(snapshot.workshopName());
        task.setLineId(dispatch.getLineId());
        task.setLineName(snapshot.lineName());
        task.setShiftId(dispatch.getShiftId());
        task.setPlanDate(dispatch.getPlanDate());
        task.setPlanQuantity(dispatch.getPlanQuantity());
        task.setPlanStartTime(dispatch.getPlanStartTime());
        task.setPlanEndTime(dispatch.getPlanEndTime());
        task.setTaskStatus(SceneTaskStatusEnum.PENDING.getStatus());
        task.setQualifiedQuantity(BigDecimal.ZERO);
        task.setDefectQuantity(BigDecimal.ZERO);
        task.setCreateBy(operatorId);
        productionTaskRepository.saveAndFlush(task);

        List<SceneProcessTaskEntity> processTasks = routeSteps.stream()
                .map(step -> toProcessTask(task.getId(), step, operatorId))
                .toList();
        processTaskRepository.saveAll(processTasks);
        return task.getId();
    }

    private SceneProcessTaskEntity toProcessTask(
            Long taskId, CraftRouteDetailEntity step, Long operatorId) {
        SceneProcessTaskEntity task = new SceneProcessTaskEntity();
        task.setProductionTaskId(taskId);
        task.setRouteDetailId(step.getId());
        task.setProcessId(step.getProcessId());
        task.setSequenceNo(step.getSequenceNo());
        task.setStationId(step.getStationId());
        task.setEquipmentCategoryId(step.getEquipmentCategoryId());
        task.setSopId(step.getSopId());
        task.setInspect(Boolean.TRUE.equals(step.getInspect()));
        task.setTaskStatus(SceneTaskStatusEnum.PENDING.getStatus());
        task.setQualifiedQuantity(BigDecimal.ZERO);
        task.setDefectQuantity(BigDecimal.ZERO);
        task.setCreateBy(operatorId);
        return task;
    }
}
