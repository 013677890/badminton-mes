package com.badminton.mes.module.production.dal.repository;

import java.math.BigDecimal;

/**
 * 欠料看板聚合投影：按物料汇总欠料量、影响工单数与在途数量。
 *
 * <p>接口投影承接 JPQL GROUP BY 结果，避免为聚合查询建实体。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public interface ShortageBoardProjection {

    /**
     * @return 物料主键
     */
    Long getMaterialId();

    /**
     * @return 欠料总量(各工单欠料数量求和)
     */
    BigDecimal getTotalShortage();

    /**
     * @return 影响工单数
     */
    Long getAffectedOrderCount();

    /**
     * @return 在途数量(各行相同，取最大值)
     */
    BigDecimal getTransitQuantity();
}
