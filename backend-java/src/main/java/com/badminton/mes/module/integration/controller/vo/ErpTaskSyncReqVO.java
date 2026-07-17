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

    /** 来源系统，未填写时由门面回退为 ERP 默认标识并参与幂等分区。 */
    @Size(max = 32, message = "来源系统长度不能超过 32")
    private String sourceSystem;

    /** 指定同步的 ERP 任务单号，为空时同步全部数据源任务。 */
    @Size(max = 64, message = "ERP 任务单号长度不能超过 64")
    private String erpOrderNo;

    /** 按任务计划开始时间筛选的闭区间起点，可空。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 按任务计划开始时间筛选的闭区间终点，可空。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
