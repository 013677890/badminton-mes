package com.badminton.mes.module.report.service.impl;

import com.badminton.mes.module.report.service.DefectSourceProvider;
import com.badminton.mes.module.report.service.dto.*;
import com.badminton.mes.module.scene.dal.repository.SceneRepairWorkOrderRepository;
import com.badminton.mes.module.scene.dal.repository.SceneProductionTaskRepository;
import java.util.ArrayList;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** B 组返修复检不良来源。 @author 刘涵 */
@Component @Order(20)
public class RepairDefectSourceProvider implements DefectSourceProvider {
    private final SceneRepairWorkOrderRepository repository;
    private final SceneProductionTaskRepository taskRepository;
    public RepairDefectSourceProvider(SceneRepairWorkOrderRepository repository, SceneProductionTaskRepository taskRepository) {
        this.repository = repository; this.taskRepository = taskRepository;
    }
    @Override
    public DefectSourceBatch load(ReportQueryCriteria criteria, int limit) {
        var records = new ArrayList<DefectSourceRecord>();
        for (var repair : repository.findAllByDeletedFalseOrderByCreatedTimeAsc()) {
            if (records.size() >= limit || repair.getUpdatedTime().isBefore(criteria.startTime())
                    || repair.getUpdatedTime().isAfter(criteria.endTime())) continue;
            var taskOptional = taskRepository.findByIdAndDeletedFalse(repair.getTaskId());
            if (taskOptional.isEmpty()) continue;
            var task = taskOptional.get();
            if (criteria.taskId() != null && !criteria.taskId().equals(task.getId())) continue;
            if (criteria.workshopId() != null && !criteria.workshopId().equals(task.getWorkshopId())) continue;
            if (criteria.lineId() != null && !criteria.lineId().equals(task.getLineId())) continue;
            if (criteria.productId() != null && !criteria.productId().equals(task.getProductId())) continue;
            if (criteria.batchNo() != null && !criteria.batchNo().equals(repair.getBatchNo())) continue;
            long occurrence = "CONTINUE_REPAIR".equals(repair.getRecheckResult())
                    || "SCRAPPED".equals(repair.getRecheckResult()) ? repair.getRecheckQuantity() : 0L;
            records.add(new DefectSourceRecord("REPAIR_RECHECK", repair.getId(), null, "REPAIR:" + repair.getId(),
                    task.getId(), task.getTaskNo(), task.getWorkOrderNo(), task.getProductId(), task.getProductName(),
                    repair.getBatchNo(), task.getWorkshopId(), task.getLineId(), null, "返修复检", null,
                    repair.getRecheckResult(), occurrence, 0L, repair.getUpdatedTime()));
        }
        return new DefectSourceBatch(records, java.util.List.of());
    }
}
