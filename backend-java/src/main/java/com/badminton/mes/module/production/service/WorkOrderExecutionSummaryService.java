package com.badminton.mes.module.production.service;

/**
 * 生产工单现场执行汇总写入契约。
 *
 * <p>B 组现场模块只通过该契约更新 A 组明确开放的投入、完工、不良和返修汇总字段，
 * 不直接访问生产工单 Repository，也不修改工单计划主数据和状态。
 *
 * @author 张竹灏
 * @date 2026/07/14
 */
public interface WorkOrderExecutionSummaryService {

    /**
     * 应用一笔报工或冲销的工单汇总增量。
     *
     * @param workOrderId 工单主键
     * @param inputDelta 投入增量，冲销时为负数
     * @param defectDelta 不良增量，冲销时为负数
     * @param reworkDelta 返修增量，冲销时为负数
     */
    void adjustReportedQuantities(Long workOrderId, int inputDelta, int defectDelta, int reworkDelta);

    /**
     * 累加一张审核通过的完工单数量。
     *
     * @param workOrderId 工单主键
     * @param completionQuantity 完工数量
     */
    void addApprovedCompletion(Long workOrderId, int completionQuantity);
}
