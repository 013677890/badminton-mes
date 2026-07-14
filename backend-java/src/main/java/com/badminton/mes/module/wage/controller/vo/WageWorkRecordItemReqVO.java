package com.badminton.mes.module.wage.controller.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 单条已审核报工快照。 */
@Data
public class WageWorkRecordItemReqVO {
    /** 来源生产报工主键 */
    @NotNull(message = "来源报工 id 不能为空")
    @Positive(message = "来源报工 id 必须为正数")
    private Long sourceReportId;
    /** 员工主键 */
    @NotNull(message = "员工 id 不能为空")
    @Positive(message = "员工 id 必须为正数")
    private Long employeeId;
    /** 作业日期 */
    @NotNull(message = "作业日期不能为空")
    private LocalDate workDate;
    /** 工单主键 */
    @NotNull(message = "工单 id 不能为空")
    @Positive(message = "工单 id 必须为正数")
    private Long workOrderId;
    /** 工序主键 */
    @NotNull(message = "工序 id 不能为空")
    @Positive(message = "工序 id 必须为正数")
    private Long processId;
    /** 产品主键 */
    @NotNull(message = "产品 id 不能为空")
    @Positive(message = "产品 id 必须为正数")
    private Long productId;
    /** 审核合格数量 */
    @NotNull(message = "合格数量不能为空")
    @DecimalMin(value = "0.0000", message = "合格数量不能小于 0")
    @Digits(integer = 8, fraction = 4, message = "合格数量最多 8 位整数和 4 位小数")
    private BigDecimal qualifiedQuantity;
    /** 审核不良数量 */
    @NotNull(message = "不良数量不能为空")
    @DecimalMin(value = "0.0000", message = "不良数量不能小于 0")
    @Digits(integer = 8, fraction = 4, message = "不良数量最多 8 位整数和 4 位小数")
    private BigDecimal defectQuantity;
    /** 来源审核时间 */
    @NotNull(message = "来源审核时间不能为空")
    private LocalDateTime sourceAuditTime;
    /** 来源是否已审核，只接受 true */
    @NotNull(message = "来源审核状态不能为空")
    @AssertTrue(message = "仅允许导入已审核报工")
    private Boolean approved;
}
