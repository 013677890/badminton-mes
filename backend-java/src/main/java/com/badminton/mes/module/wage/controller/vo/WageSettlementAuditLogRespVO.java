package com.badminton.mes.module.wage.controller.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 工资结算审计日志响应。 */
@Data
public class WageSettlementAuditLogRespVO {
    /** 主键 */
    private Long id;
    /** 明细主键 */
    private Long detailId;
    /** 动作类型 */
    private String actionType;
    /** 原状态 */
    private Integer fromStatus;
    /** 目标状态 */
    private Integer toStatus;
    /** 调整前金额，单位元 */
    private BigDecimal beforeAmount;
    /** 调整后金额，单位元 */
    private BigDecimal afterAmount;
    /** 操作原因 */
    private String actionReason;
    /** 操作人 */
    private Long operateBy;
    /** 操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operateTime;
}
