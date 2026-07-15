package com.badminton.mes.module.barcode.controller.vo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 条码模板字段请求 VO，随模板保存请求提交。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeTemplateFieldSaveReqVO {

    /** 字段名称，如条码值/产品名称/批次号/生产日期/工单号 */
    @NotBlank(message = "字段名称不能为空")
    @Size(max = 64, message = "字段名称长度不能超过 64")
    private String fieldName;

    /** 字段类型：1 文本 2 条码 3 二维码 */
    @NotNull(message = "字段类型不能为空")
    @Min(value = 1, message = "字段类型取值为 1-3")
    @Max(value = 3, message = "字段类型取值为 1-3")
    private Integer fieldType;

    /** 数据来源字段 */
    @NotBlank(message = "数据来源不能为空")
    @Size(max = 64, message = "数据来源长度不能超过 64")
    private String dataSource;

    /** X 位置(mm) */
    @NotNull(message = "X 位置不能为空")
    @DecimalMin(value = "0", message = "X 位置不能为负数")
    @Digits(integer = 4, fraction = 2, message = "X 位置整数最多 4 位、小数最多 2 位")
    private BigDecimal posX;

    /** Y 位置(mm) */
    @NotNull(message = "Y 位置不能为空")
    @DecimalMin(value = "0", message = "Y 位置不能为负数")
    @Digits(integer = 4, fraction = 2, message = "Y 位置整数最多 4 位、小数最多 2 位")
    private BigDecimal posY;

    /** 字体大小，可空 */
    @Min(value = 1, message = "字体大小最小值为 1")
    @Max(value = 255, message = "字体大小最大值为 255")
    private Integer fontSize;
}
