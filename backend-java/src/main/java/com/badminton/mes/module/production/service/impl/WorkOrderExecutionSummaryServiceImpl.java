package com.badminton.mes.module.production.service.impl;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.dal.redis.WorkOrderCache;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.service.WorkOrderExecutionSummaryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 生产工单现场执行汇总写入实现。
 *
 * @author 张竹灏
 * @date 2026/07/14
 */
@Service
public class WorkOrderExecutionSummaryServiceImpl implements WorkOrderExecutionSummaryService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderCache workOrderCache;

    public WorkOrderExecutionSummaryServiceImpl(WorkOrderRepository workOrderRepository,
                                                WorkOrderCache workOrderCache) {
        this.workOrderRepository = workOrderRepository;
        this.workOrderCache = workOrderCache;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustReportedQuantities(Long workOrderId, int inputDelta,
                                         int defectDelta, int reworkDelta) {
        int updated = workOrderRepository.adjustExecutionSummary(
                workOrderId, inputDelta, defectDelta, reworkDelta);
        if (updated == 0) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_EXECUTION_QUANTITY_INVALID);
        }
        workOrderCache.evictAfterCommit(workOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addApprovedCompletion(Long workOrderId, int completionQuantity) {
        if (completionQuantity <= 0) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_EXECUTION_QUANTITY_INVALID);
        }
        int updated = workOrderRepository.increaseApprovedFinishQuantity(workOrderId, completionQuantity);
        if (updated == 0) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_EXECUTION_QUANTITY_INVALID);
        }
        workOrderCache.evictAfterCommit(workOrderId);
    }
}
