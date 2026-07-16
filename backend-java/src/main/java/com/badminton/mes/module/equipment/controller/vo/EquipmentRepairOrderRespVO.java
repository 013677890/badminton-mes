package com.badminton.mes.module.equipment.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 设备报修任务响应 VO，详情与分页列表共用。
 *
 * <p>返回一次故障从上报、派工、维修到终态的完整业务快照，包括关联设备、故障分类、处理人员、
 * 时间和结果；逻辑删除标记等持久化控制字段不向客户端暴露。
 *
 * @author 角色C
 * @date 2026/07/10
 */
@Data
public class EquipmentRepairOrderRespVO {

    /** 设备报修任务主键。 */
    private Long id;

    /** 业务唯一的报修单号。 */
    private String repairNo;

    /** 发生故障的设备台账主键。 */
    private Long equipmentId;

    /** 匹配的故障原理主键，未分类时为空。 */
    private Long faultPrincipleId;

    /** 报修时记录的现场故障现象和影响。 */
    private String faultDescription;

    /** 故障上报时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reportTime;

    /** 发起报修的系统用户主键。 */
    private Long reportUserId;

    /** 被指派的维修人用户主键，尚未派工时为空。 */
    private Long repairUserId;

    /** 实际维修开始时间，尚未开始维修时为空。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime repairStartTime;

    /** 实际维修结束时间，任务未完成时为空。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime repairEndTime;

    /** 维修过程、处理措施及最终结果。 */
    private String repairResult;

    /** 任务状态：REPORTED、ASSIGNED、REPAIRING、FINISHED 或 CANCELLED。 */
    private String repairStatus;

    /** 报修任务的其他补充说明。 */
    private String remark;

    /** 报修任务创建时间，由数据库维护。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 报修任务最后更新时间，由数据库维护。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
