package com.badminton.mes.module.production.dal.redis;

/**
 * 派工单号流水生成器。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public interface DispatchNoSequence {

    /**
     * 生成下一个派工单号。
     *
     * @return 派工单号
     */
    String nextNo();
}
