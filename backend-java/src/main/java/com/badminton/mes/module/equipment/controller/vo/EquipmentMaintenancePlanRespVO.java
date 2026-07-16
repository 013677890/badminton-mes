package com.badminton.mes.module.equipment.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 设备保养计划响应 VO，供详情与分页列表复用。
 *
 * <p>返回计划定义、责任人和最近/下次执行时间，不暴露逻辑删除标记等持久化控制字段。时间字段统一
 * 输出到秒，便于前端直接展示和构造计划日历。
 */
@Data
public class EquipmentMaintenancePlanRespVO {

    /** 保养计划主键。 */
    private Long id;

    /** 业务唯一的计划编码。 */
    private String planCode;

    /** 面向业务人员展示的计划名称。 */
    private String planName;

    /** 计划绑定的设备台账主键。 */
    private Long equipmentId;

    /** 保养类型：ROUTINE、PREVENTIVE 或 SPECIAL。 */
    private String maintenanceType;

    /** 两次计划保养之间的自然日间隔。 */
    private Integer cycleDays;

    /** 计划要求执行的标准保养内容。 */
    private String maintenanceContent;

    /** 默认负责该计划的已启用系统用户主键，可空。 */
    private Long responsibleUserId;

    /** 最近一次已完成保养任务的完成时间；尚无完成记录时为空。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMaintenanceTime;

    /** 下一次应执行保养的时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextMaintenanceTime;

    /** 计划补充说明。 */
    private String remark;

    /** 计划状态：1 启用，0 停用。 */
    private Integer status;

    /** 计划创建时间，由数据库维护。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 计划最后更新时间，由数据库维护。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
