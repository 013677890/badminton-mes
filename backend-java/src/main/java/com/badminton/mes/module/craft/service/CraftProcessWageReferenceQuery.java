package com.badminton.mes.module.craft.service;

/** 工序对计件工资规则的反向引用查询契约。 */
public interface CraftProcessWageReferenceQuery {

    /**
     * 判断工序是否仍被启用的计件规则引用。
     *
     * @param processId 工序主键
     * @return true 表示存在启用的计件规则
     */
    boolean hasEnabledPieceRateRule(Long processId);
}
