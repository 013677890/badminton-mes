package com.badminton.mes.module.production.controller.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 派工单创建/修改请求 VO。
 *
 * <p>修改已下发的派工单时 adjustReason 必填(Service 校验)；
 * suggest 标记是否采纳系统建议创建。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
public class DispatchSaveReqVO {

    /** 来源生产工单 id(修改时忽略，不允许换工单) */
    @NotNull(message = "生产工单不能为空")
    @Positive(message = "生产工单 id 必须为正数")
    private Long workOrderId;

    /** 产线 id */
    @NotNull(message = "产线不能为空")
    @Positive(message = "产线 id 必须为正数")
    private Long lineId;

    /** 班次 id */
    @NotNull(message = "班次不能为空")
    @Positive(message = "班次 id 必须为正数")
    private Long shiftId;

    /** 排产日期 */
    @NotNull(message = "排产日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDate;

    /** 计划数量(不超工单未派数量) */
    @NotNull(message = "计划数量不能为空")
    @Positive(message = "计划数量必须为正数")
    private Integer planQuantity;

    /** 计划开始时间 */
    @NotNull(message = "计划开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planStartTime;

    /** 计划结束时间 */
    @NotNull(message = "计划结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planEndTime;

    /** 是否采纳系统建议创建，可空(默认人工) */
    private Boolean suggest;

    /** 调整原因，修改已下发派工单时必填 */
    @Size(max = 255, message = "调整原因长度不能超过 255")
    private String adjustReason;
}
