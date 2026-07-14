package com.badminton.mes.module.wage.service;

/**
 * 工资模块提供的产品计件规则反向引用查询契约。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
public interface ProductPieceRateRuleReferenceQuery {

    /**
     * 判断产品是否被启用计件规则引用。
     *
     * @param productId 产品主键
     * @return true 表示存在启用规则
     */
    boolean hasEnabledRule(Long productId);

    /**
     * 判断产品是否被任意未删除计件规则引用。
     *
     * @param productId 产品主键
     * @return true 表示存在计件规则
     */
    boolean hasAnyRule(Long productId);
}
