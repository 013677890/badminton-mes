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

/**
 * 设备保养计划创建/修改请求 VO。
 *
 * <p>注解负责格式、长度和必填项等单字段约束。计划编码唯一性、设备是否可用、负责人是否为启用用户，
 * 以及已有历史记录时能否更换设备均属于需要访问持久层的业务规则，由 Service 在事务内校验。
 */
@Data
public class EquipmentMaintenancePlanSaveReqVO {

    /** 业务唯一的计划编码；禁止使用逻辑删除记录保留的系统前缀。 */
    @NotBlank(message = "保养计划编码不能为空")
    @Size(max = 32, message = "保养计划编码长度不能超过 32")
    @Pattern(regexp = "^(?!__DELETED_).+$", message = "保养计划编码不能使用系统保留前缀")
    private String planCode;

    /** 面向执行人员展示的计划名称。 */
    @NotBlank(message = "保养计划名称不能为空")
    @Size(max = 128, message = "保养计划名称长度不能超过 128")
    private String planName;

    /** 计划绑定的设备台账主键，设备必须存在且未报废。 */
    @NotNull(message = "保养设备不能为空")
    @Positive(message = "保养设备必须为正整数")
    private Long equipmentId;

    /** 保养类型，可空；创建时由 Service 补充默认类型 ROUTINE。 */
    @Pattern(regexp = "^(ROUTINE|PREVENTIVE|SPECIAL)$",
             message = "保养类型必须为 ROUTINE、PREVENTIVE、SPECIAL 之一")
    private String maintenanceType;

    /** 保养周期天数，用于完成任务后推算下一次保养时间。 */
    @NotNull(message = "保养周期不能为空")
    @Min(value = 1, message = "保养周期最少为 1 天")
    @Max(value = 3650, message = "保养周期最多为 3650 天")
    private Integer cycleDays;

    /** 每次任务应遵循的标准保养内容。 */
    @NotBlank(message = "保养内容不能为空")
    @Size(max = 500, message = "保养内容长度不能超过 500")
    private String maintenanceContent;

    /** 默认负责人用户主键，可空；非空时必须指向启用且未删除的用户。 */
    @Positive(message = "负责人必须为正整数")
    private Long responsibleUserId;

    /** 首次或尚无完成记录时使用的下次计划保养时间。 */
    @NotNull(message = "下次保养时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextMaintenanceTime;

    /** 计划补充说明，可空。 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    /** 启停状态，可空；创建时由 Service 补充默认启用状态。 */
    @Min(value = 0, message = "状态只能为 0 或 1")
    @Max(value = 1, message = "状态只能为 0 或 1")
    private Integer status;
}
