package com.badminton.mes.module.equipment.controller.vo;

import java.time.LocalDateTime;

import com.badminton.mes.common.core.PageParam;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 设备保养计划分页查询请求 VO。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EquipmentMaintenancePlanPageReqVO extends PageParam {

    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    @Positive(message = "设备必须为正整数")
    private Long equipmentId;

    @Pattern(regexp = "^(ROUTINE|PREVENTIVE|SPECIAL)$",
             message = "保养类型必须为 ROUTINE、PREVENTIVE、SPECIAL 之一")
    private String maintenanceType;

    @Min(value = 0, message = "状态只能为 0 或 1")
    @Max(value = 1, message = "状态只能为 0 或 1")
    private Integer status;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextMaintenanceStartTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextMaintenanceEndTime;
}
