package com.badminton.mes.module.production.controller.vo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 工单进度批量查询响应 VO。
 *
 * <p>面向报表/看板的轻量出参，避免逐条调详情接口。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
public class WorkOrderProgressRespVO {

    /** 工单主键 */
    private Long id;

    /** 工单号 */
    private String workOrderNo;

    /** 产品名称 */
    private String productName;

    /** 计划数量 */
    private Integer planQuantity;

    /** 已派工数量 */
    private Integer dispatchedQuantity;

    /** 投入数量 */
    private Integer inputQuantity;

    /** 完工数量 */
    private Integer finishQuantity;

    /** 不良数量 */
    private Integer defectQuantity;

    /** 返修数量 */
    private Integer reworkQuantity;

    /** 工单状态 */
    private Integer orderStatus;

    /** 进度百分比：完工数量 / 计划数量 × 100，计划为 0 时返回 0 */
    private BigDecimal progressPercent;
}
