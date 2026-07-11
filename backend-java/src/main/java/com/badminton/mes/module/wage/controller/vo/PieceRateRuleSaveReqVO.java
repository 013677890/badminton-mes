package com.badminton.mes.module.wage.controller.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 计件规则创建与修改请求。 */
@Data
public class PieceRateRuleSaveReqVO {
    /** 工序主键 */
    @NotNull(message = "工序不能为空")
    @Positive(message = "工序 id 必须为正数")
    private Long processId;
    /** 产品主键，空表示工序通用规则 */
    @Positive(message = "产品 id 必须为正数")
    private Long productId;
    /** 单价，单位元，最多四位小数 */
    @NotNull(message = "计件单价不能为空")
    @DecimalMin(value = "0.0001", message = "计件单价必须大于 0")
    @DecimalMax(value = "1000.0000", message = "计件单价不能超过 1000 元")
    @Digits(integer = 4, fraction = 4, message = "计件单价最多 4 位整数和 4 位小数")
    private BigDecimal unitPrice;
    /** 不良扣减率，百分比 */
    @NotNull(message = "不良扣减率不能为空")
    @DecimalMin(value = "0.00", message = "不良扣减率不能小于 0")
    @DecimalMax(value = "100.00", message = "不良扣减率不能大于 100")
    @Digits(integer = 3, fraction = 2, message = "不良扣减率最多保留两位小数")
    private BigDecimal defectDeductionRate;
    /** 生效开始日期 */
    @NotNull(message = "生效开始日期不能为空")
    private LocalDate effectiveStart;
    /** 生效结束日期，空表示长期 */
    private LocalDate effectiveEnd;
    /** 状态：1 启用 0 停用 */
    @NotNull(message = "规则状态不能为空")
    @Min(value = 0, message = "规则状态不合法")
    @Max(value = 1, message = "规则状态不合法")
    private Integer status;
    /** 变更原因 */
    @Size(max = 255, message = "变更原因长度不能超过 255")
    private String changeReason;
}
