package com.badminton.mes.module.production.dal.redis;

/**
 * 工单号流水生成器。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
public interface WorkOrderNoSequence {

    /**
     * 生成下一个工单号。
     *
     * @return 工单号
     */
    String nextNo();
}
