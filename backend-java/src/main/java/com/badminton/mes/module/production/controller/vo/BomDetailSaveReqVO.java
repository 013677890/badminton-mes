package com.badminton.mes.module.production.controller.vo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** BOM 物料明细保存请求。 */
@Data
public class BomDetailSaveReqVO {
    /** 物料主键 */
    @NotNull(message = "物料不能为空")
    @Positive(message = "物料 id 必须为正数")
    private Long materialId;
    /** 单位产品标准用量 */
    @NotNull(message = "标准用量不能为空")
    @DecimalMin(value = "0.0001", message = "标准用量必须大于 0")
    @Digits(integer = 8, fraction = 4, message = "标准用量最多 8 位整数和 4 位小数")
    private BigDecimal quantity;
    /** 损耗率百分比 */
    @NotNull(message = "损耗率不能为空")
    @DecimalMin(value = "0.00", message = "损耗率不能小于 0")
    @DecimalMax(value = "100.00", message = "损耗率不能大于 100")
    @Digits(integer = 3, fraction = 2, message = "损耗率最多保留两位小数")
    private BigDecimal lossRate;
}
