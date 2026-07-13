package com.badminton.mes.module.integration.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ERP 生产任务单同步触发请求。
 *
 * <p>所有字段可选：不传参数时同步数据源全部数据；可按单号或计划开始时间范围筛选。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
public class ErpTaskSyncReqVO {

    /** 来源系统，默认 ERP */
    @Size(max = 32, message = "来源系统长度不能超过 32")
    private String sourceSystem;

    /** 指定同步的 ERP 任务单号，为空时同步全部 */
    @Size(max = 64, message = "ERP 任务单号长度不能超过 64")
    private String erpOrderNo;

    /** 同步起始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 同步截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
