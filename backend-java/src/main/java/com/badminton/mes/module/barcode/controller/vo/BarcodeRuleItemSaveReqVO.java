package com.badminton.mes.module.barcode.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 条码规则组成项请求 VO，随规则保存/预览/校验请求提交。
 *
 * <p>单字段规则用注解声明；"常量必须有值、日期格式合法、变量名受支持、
 * 必须且只能包含一个流水号"等组合规则由 {@code BarcodeValueComposer} 校验。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeRuleItemSaveReqVO {

    /** 组成顺序，同规则内唯一 */
    @NotNull(message = "组成顺序不能为空")
    @Min(value = 1, message = "组成顺序最小值为 1")
    @Max(value = 255, message = "组成顺序最大值为 255")
    private Integer seq;

    /** 组成类型：1 常量 2 日期 3 变量 4 流水号 */
    @NotNull(message = "组成类型不能为空")
    @Min(value = 1, message = "组成类型取值为 1-4")
    @Max(value = 4, message = "组成类型取值为 1-4")
    private Integer itemType;

    /** 常量值(类型=常量)或变量名(类型=变量，支持 productCode/lineCode) */
    @Size(max = 64, message = "常量值或变量名长度不能超过 64")
    private String itemValue;

    /** 日期格式(类型=日期时必填，如 yyyyMMdd) */
    @Size(max = 16, message = "日期格式长度不能超过 16")
    private String dateFormat;

    /** 该段长度，可空；流水号段以规则流水位数为准 */
    @Min(value = 1, message = "段长度最小值为 1")
    @Max(value = 64, message = "段长度最大值为 64")
    private Integer itemLength;
}
