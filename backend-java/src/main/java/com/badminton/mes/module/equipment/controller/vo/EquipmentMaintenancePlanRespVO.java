package com.badminton.mes.module.equipment.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 设备保养计划响应 VO。 */
@Data
public class EquipmentMaintenancePlanRespVO {

    private Long id;
    private String planCode;
    private String planName;
    private Long equipmentId;
    private String maintenanceType;
    private Integer cycleDays;
    private String maintenanceContent;
    private Long responsibleUserId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMaintenanceTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextMaintenanceTime;

    private String remark;
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
