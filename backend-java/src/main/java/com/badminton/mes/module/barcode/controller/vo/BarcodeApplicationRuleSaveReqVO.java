package com.badminton.mes.module.barcode.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 条码应用规则创建/修改请求 VO。
 *
 * <p>"对象类型与产品/物料匹配、来源为规则生成时规则必填、同对象同类型
 * 仅一条启用默认规则"等组合规则由 Service 校验；规则版本由系统管理。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeApplicationRuleSaveReqVO {

    /** 对象类型：1 产品 2 物料 */
    @NotNull(message = "对象类型不能为空")
    @Min(value = 1, message = "对象类型取值为 1-2")
    @Max(value = 2, message = "对象类型取值为 1-2")
    private Integer objectType;

    /** 适用产品 id，对象类型=1 时必填 */
    @Positive(message = "产品 id 必须为正数")
    private Long productId;

    /** 适用物料 id，对象类型=2 时必填 */
    @Positive(message = "物料 id 必须为正数")
    private Long materialId;

    /** 条码类型 id */
    @NotNull(message = "条码类型不能为空")
    @Positive(message = "条码类型 id 必须为正数")
    private Long barcodeTypeId;

    /** 条码模式：1 唯一码 2 批次码(第一阶段以批次码为主) */
    @NotNull(message = "条码模式不能为空")
    @Min(value = 1, message = "条码模式取值为 1-2")
    @Max(value = 2, message = "条码模式取值为 1-2")
    private Integer barcodeMode;

    /** 条码规则 id，来源=规则生成时必填 */
    @Positive(message = "条码规则 id 必须为正数")
    private Long ruleId;

    /** 标签模板 id(具体版本行) */
    @NotNull(message = "标签模板不能为空")
    @Positive(message = "标签模板 id 必须为正数")
    private Long templateId;

    /** 条码来源：1 规则生成 2 传入值生成 3 外部导入 */
    @NotNull(message = "条码来源不能为空")
    @Min(value = 1, message = "条码来源取值为 1-3")
    @Max(value = 3, message = "条码来源取值为 1-3")
    private Integer sourceType;

    /** 是否默认规则，可空，默认 true(同对象同类型仅一条启用默认规则) */
    private Boolean defaultFlag;
}
