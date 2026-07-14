package com.badminton.mes.module.equipment.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 设备保养计划创建/修改请求 VO。 */
@Data
public class EquipmentMaintenancePlanSaveReqVO {

    @NotBlank(message = "保养计划编码不能为空")
    @Size(max = 32, message = "保养计划编码长度不能超过 32")
    @Pattern(regexp = "^(?!__DELETED_).+$", message = "保养计划编码不能使用系统保留前缀")
    private String planCode;

    @NotBlank(message = "保养计划名称不能为空")
    @Size(max = 128, message = "保养计划名称长度不能超过 128")
    private String planName;

    @NotNull(message = "保养设备不能为空")
    @Positive(message = "保养设备必须为正整数")
    private Long equipmentId;

    @Pattern(regexp = "^(ROUTINE|PREVENTIVE|SPECIAL)$",
             message = "保养类型必须为 ROUTINE、PREVENTIVE、SPECIAL 之一")
    private String maintenanceType;

    @NotNull(message = "保养周期不能为空")
    @Min(value = 1, message = "保养周期最少为 1 天")
    @Max(value = 3650, message = "保养周期最多为 3650 天")
    private Integer cycleDays;

    @NotBlank(message = "保养内容不能为空")
    @Size(max = 500, message = "保养内容长度不能超过 500")
    private String maintenanceContent;

    @Positive(message = "负责人必须为正整数")
    private Long responsibleUserId;

    @NotNull(message = "下次保养时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextMaintenanceTime;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    @Min(value = 0, message = "状态只能为 0 或 1")
    @Max(value = 1, message = "状态只能为 0 或 1")
    private Integer status;
}
