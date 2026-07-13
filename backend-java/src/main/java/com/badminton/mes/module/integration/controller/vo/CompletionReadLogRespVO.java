package com.badminton.mes.module.integration.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 生产完工单读取日志分页行响应。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
public class CompletionReadLogRespVO {

    /** 日志主键 */
    private Long id;

    /** 完工单主键 */
    private Long completionOrderId;

    /** 完工单号 */
    private String completionNo;

    /** 生产工单号 */
    private String workOrderNo;

    /** 读取来源系统 */
    private String sourceSystem;

    /** 调用用户 */
    private Long readBy;

    /** 读取时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;
}
