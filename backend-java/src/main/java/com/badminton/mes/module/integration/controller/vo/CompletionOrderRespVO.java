package com.badminton.mes.module.integration.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 已审核生产完工单响应。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
public class CompletionOrderRespVO {

    /** 完工单主键 */
    private Long id;

    /** 完工单号 */
    private String completionNo;

    /** 现场生产任务主键，历史完工单可空 */
    private Long productionTaskId;

    /** 生产工单号 */
    private String workOrderNo;

    /** 产品编码 */
    private String productCode;

    /** 产品名称 */
    private String productName;

    /** 产品批次号 */
    private String batchNo;

    /** 完工数量 */
    private Integer completionQuantity;

    /** 良品数量 */
    private Integer goodQuantity;

    /** 不良数量 */
    private Integer defectQuantity;

    /** 审核状态，固定为 1 */
    private Integer auditStatus;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
