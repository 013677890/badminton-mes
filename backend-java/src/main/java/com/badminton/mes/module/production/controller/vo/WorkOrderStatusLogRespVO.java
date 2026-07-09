package com.badminton.mes.module.production.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 工单状态日志响应 VO。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
@Data
public class WorkOrderStatusLogRespVO {

    /** 主键 */
    private Long id;

    /** 生产工单 id */
    private Long workOrderId;

    /** 变更前状态 */
    private Integer fromStatus;

    /** 变更后状态 */
    private Integer toStatus;

    /** 变更类型：1 状态流转 2 计划变更 */
    private Integer changeType;

    /** 变更原因 */
    private String changeReason;

    /** 操作人用户 id */
    private Long operateBy;

    /** 操作时间，格式 yyyy-MM-dd HH:mm:ss(API-013) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operateTime;
}
