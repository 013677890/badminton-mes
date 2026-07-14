package com.badminton.mes.module.equipment.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 设备报修任务响应 VO，详情与分页列表共用。
 *
 * @author 角色C
 * @date 2026/07/10
 */
@Data
public class EquipmentRepairOrderRespVO {

    /** 报修任务主键 */
    private Long id;

    /** 报修单号 */
    private String repairNo;

    /** 设备台账 id */
    private Long equipmentId;

    /** 故障原理 id */
    private Long faultPrincipleId;

    /** 故障描述 */
    private String faultDescription;

    /** 报修时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reportTime;

    /** 报修人用户 id */
    private Long reportUserId;

    /** 维修人用户 id */
    private Long repairUserId;

    /** 维修开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime repairStartTime;

    /** 维修结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime repairEndTime;

    /** 维修结果 */
    private String repairResult;

    /** 报修状态 */
    private String repairStatus;

    /** 备注说明 */
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
