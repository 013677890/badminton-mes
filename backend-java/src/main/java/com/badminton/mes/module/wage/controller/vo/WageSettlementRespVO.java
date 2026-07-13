package com.badminton.mes.module.wage.controller.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 工资结算批次响应。 */
@Data
public class WageSettlementRespVO {
    /** 主键 */
    private Long id;
    /** 结算批次号 */
    private String settlementNo;
    /** 周期开始日期 */
    private LocalDate periodStart;
    /** 周期结束日期 */
    private LocalDate periodEnd;
    /** 结算状态 */
    private Integer settlementStatus;
    /** 合格数量合计 */
    private BigDecimal totalQualifiedQuantity;
    /** 不良数量合计 */
    private BigDecimal totalDefectQuantity;
    /** 最终金额，单位元 */
    private BigDecimal totalAmount;
    /** 乐观锁版本 */
    private Integer version;
    /** 提交人 */
    private Long submitBy;
    /** 提交时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submitTime;
    /** 审核人 */
    private Long auditBy;
    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;
    /** 审核意见 */
    private String auditReason;
    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
