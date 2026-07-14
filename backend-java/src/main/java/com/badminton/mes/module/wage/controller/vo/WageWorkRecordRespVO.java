package com.badminton.mes.module.wage.controller.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 报工计件快照响应。 */
@Data
public class WageWorkRecordRespVO {
    /** 主键 */
    private Long id;
    /** 来源报工主键 */
    private Long sourceReportId;
    /** 员工主键 */
    private Long employeeId;
    /** 作业日期 */
    private LocalDate workDate;
    /** 工单主键 */
    private Long workOrderId;
    /** 工序主键 */
    private Long processId;
    /** 产品主键 */
    private Long productId;
    /** 合格数量 */
    private BigDecimal qualifiedQuantity;
    /** 不良数量 */
    private BigDecimal defectQuantity;
    /** 来源审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sourceAuditTime;
    /** 导入时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
