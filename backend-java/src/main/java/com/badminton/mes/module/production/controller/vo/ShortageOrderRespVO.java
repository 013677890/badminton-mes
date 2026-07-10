package com.badminton.mes.module.production.controller.vo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 欠料看板下钻响应 VO：某欠料物料影响的工单行。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
public class ShortageOrderRespVO {

    /** 生产工单 id */
    private Long workOrderId;

    /** 工单号 */
    private String workOrderNo;

    /** 产品名称(工单冗余字段) */
    private String productName;

    /** 需求数量(剩余需求) */
    private BigDecimal requireQuantity;

    /** 可用数量 */
    private BigDecimal availableQuantity;

    /** 欠料数量 */
    private BigDecimal shortageQuantity;
}
