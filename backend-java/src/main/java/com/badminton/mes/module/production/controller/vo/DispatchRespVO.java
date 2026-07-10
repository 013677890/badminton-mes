package com.badminton.mes.module.production.controller.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 派工单响应 VO。
 *
 * <p>工单号/产品名/产线名/班次名由服务端回填；kitStatus 透出工单齐套状态，
 * 未齐套派工由前端警示(需求：未齐套可生成待确认派工但不建议开工)。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
public class DispatchRespVO {

    /** 主键 */
    private Long id;

    /** 派工单号 */
    private String dispatchNo;

    /** 来源生产工单 id */
    private Long workOrderId;

    /** 工单号 */
    private String workOrderNo;

    /** 产品名称(工单冗余) */
    private String productName;

    /** 工单齐套状态：0 未分析 1 齐套 2 部分齐套 3 欠料 */
    private Integer kitStatus;

    /** 产线 id */
    private Long lineId;

    /** 产线名称 */
    private String lineName;

    /** 班次 id */
    private Long shiftId;

    /** 班次名称 */
    private String shiftName;

    /** 排产日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDate;

    /** 计划数量 */
    private Integer planQuantity;

    /** 计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planStartTime;

    /** 计划结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planEndTime;

    /** 是否系统建议排产：1 是 0 人工 */
    private Integer suggest;

    /** 状态：0 待审核 1 已审核 2 已下发 3 执行中 4 已完成 5 已取消 */
    private Integer dispatchStatus;

    /** 审核人 */
    private Long auditBy;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    /** 下发后调整原因 */
    private String adjustReason;

    /** 创建人 */
    private Long createBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
