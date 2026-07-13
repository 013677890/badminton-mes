package com.badminton.mes.module.barcode.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 批次码批量生成请求 VO。
 *
 * <p>批量生成走同一取号与落库路径，单次上限 500 与外部导入一致，
 * 防止单请求长事务(M1 待确认事项②口径)。传入值生成来源不支持批量。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BarcodeBatchGenerateReqVO extends BarcodeGenerateReqVO {

    /** 单次批量生成上限 */
    public static final int MAX_QUANTITY = 500;

    /** 生成数量 */
    @NotNull(message = "生成数量不能为空")
    @Min(value = 1, message = "生成数量最小值为 1")
    @Max(value = MAX_QUANTITY, message = "单次生成数量不能超过 500")
    private Integer quantity;
}
