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
        // 由 Repository 使用带下限条件的原子 UPDATE 同时调整执行汇总；影响行数为 0 表示工单不存在、状态不允许
        // 或扣减会造成负数，此时抛错并回滚，避免先读后写产生并发超扣。
        int updated = workOrderRepository.adjustExecutionSummary(
                workOrderId, inputDelta, defectDelta, reworkDelta);
        if (updated == 0) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_EXECUTION_QUANTITY_INVALID);
        }
        // 工单详情缓存包含执行数量，必须在事务提交后失效，防止提交前回源把旧快照重新写回 Redis。
        workOrderCache.evictAfterCommit(workOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addApprovedCompletion(Long workOrderId, int completionQuantity) {
        // 完工回写只接受正数；上限、状态和存在性由数据库 UPDATE 条件一并校验。
        if (completionQuantity <= 0) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_EXECUTION_QUANTITY_INVALID);
        }
        int updated = workOrderRepository.increaseApprovedFinishQuantity(workOrderId, completionQuantity);
        if (updated == 0) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_EXECUTION_QUANTITY_INVALID);
        }
        // 原子更新成功后延迟清除缓存，使后续读取到提交后的完工汇总。
        workOrderCache.evictAfterCommit(workOrderId);
    }
}
