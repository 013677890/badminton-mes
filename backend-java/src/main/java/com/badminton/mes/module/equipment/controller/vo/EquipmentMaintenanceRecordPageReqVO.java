package com.badminton.mes.module.equipment.controller.vo;

import java.time.LocalDateTime;

import com.badminton.mes.common.core.PageParam;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 设备保养记录分页查询请求 VO。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EquipmentMaintenanceRecordPageReqVO extends PageParam {

    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    @Positive(message = "保养计划必须为正整数")
    private Long planId;

    @Positive(message = "设备必须为正整数")
    private Long equipmentId;

    @Pattern(regexp = "^(PENDING|IN_PROGRESS|COMPLETED|CANCELLED)$",
             message = "任务状态必须为 PENDING、IN_PROGRESS、COMPLETED、CANCELLED 之一")
    private String recordStatus;

    @Pattern(regexp = "^(NORMAL|ABNORMAL)$", message = "保养结果必须为 NORMAL 或 ABNORMAL")
    private String maintenanceResult;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledStartTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledEndTime;
}
