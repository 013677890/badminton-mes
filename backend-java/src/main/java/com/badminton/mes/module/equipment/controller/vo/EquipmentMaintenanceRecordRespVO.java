package com.badminton.mes.module.equipment.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 设备保养记录响应 VO，供任务详情与分页列表复用。
 *
 * <p>记录展示一次保养任务从计划时间到实际执行结果的完整业务快照。设备恢复所需的内部状态、逻辑
 * 删除标记等实现字段不向客户端暴露。
 */
@Data
public class EquipmentMaintenanceRecordRespVO {

    /** 保养记录主键。 */
    private Long id;

    /** 业务唯一的保养任务编号。 */
    private String recordNo;

    /** 产生该任务的保养计划主键。 */
    private Long planId;

    /** 执行保养的设备台账主键。 */
    private Long equipmentId;

    /** 计划执行时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledTime;

    /** 实际开始时间；任务进入 IN_PROGRESS 时可由服务自动补充。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 实际完成时间；任务进入 COMPLETED 时可由服务自动补充。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;

    /** 实际执行人用户主键。 */
    private Long executorUserId;

    /** 本次实际执行的保养内容。 */
    private String maintenanceContent;

    /** 保养结论：NORMAL 或 ABNORMAL，未完成时可空。 */
    private String maintenanceResult;

    /** 任务状态：PENDING、IN_PROGRESS、COMPLETED 或 CANCELLED。 */
    private String recordStatus;

    /** 异常保养结论的原因或现象说明。 */
    private String abnormalDescription;

    /** 本次任务的补充说明。 */
    private String remark;

    /** 记录创建时间，由数据库维护。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 记录最后更新时间，由数据库维护。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
