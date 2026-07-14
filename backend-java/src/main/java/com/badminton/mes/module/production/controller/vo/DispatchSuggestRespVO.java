package com.badminton.mes.module.production.controller.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 排产建议响应 VO(只读建议，不落库)。
 *
 * <p>用户采纳后前端携本 VO 参数调创建接口(suggest=true)。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
public class DispatchSuggestRespVO {

    /** 来源生产工单 id */
    private Long workOrderId;

    /** 工单齐套状态：0 未分析 1 齐套 2 部分齐套 3 欠料(欠料时前端警示) */
    private Integer kitStatus;

    /** 建议产线 id */
    private Long lineId;

    /** 建议产线名称 */
    private String lineName;

    /** 建议班次 id */
    private Long shiftId;

    /** 建议班次名称 */
    private String shiftName;

    /** 建议排产日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDate;

    /** 建议计划开始时间(planDate + 班次开始时间) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planStartTime;

    /** 建议计划结束时间(班次结束不晚于开始视为跨天，日期 +1) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planEndTime;

    /** 建议数量(不超该格剩余产能与工单剩余可派) */
    private Integer planQuantity;

    /** 交期内能否排完剩余可派数量(false 表示格子用尽仍有缺口) */
    private Boolean canFinishOnTime;
}
