package com.badminton.mes.module.wage.controller.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 计件规则响应。 */
@Data
public class PieceRateRuleRespVO {
    /** 主键 */
    private Long id;
    /** 工序主键 */
    private Long processId;
    /** 产品主键 */
    private Long productId;
    /** 单价，单位元 */
    private BigDecimal unitPrice;
    /** 不良扣减率，百分比 */
    private BigDecimal defectDeductionRate;
    /** 生效开始日期 */
    private LocalDate effectiveStart;
    /** 生效结束日期 */
    private LocalDate effectiveEnd;
    /** 状态 */
    private Integer status;
    /** 乐观锁版本 */
    private Integer version;
    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
