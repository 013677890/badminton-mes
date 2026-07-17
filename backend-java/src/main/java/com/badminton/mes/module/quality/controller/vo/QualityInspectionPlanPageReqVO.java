package com.badminton.mes.module.quality.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检验标准方案分页筛选请求。
 *
 * <p>所有业务条件均可选，可组合检索方案编码、名称、适用范围、检验类型和生命周期状态；分页参数及
 * 边界继承自 {@link PageParam}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QualityInspectionPlanPageReqVO extends PageParam {

    /** 方案编码或方案名称的模糊搜索关键字，最长 64 个字符。 */
    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    /** 适用产品主键精确条件；填写时必须为正整数。 */
    @Positive(message = "适用产品必须为正整数")
    private Long productId;

    /** 适用客户主键精确条件；填写时必须为正整数。 */
    @Positive(message = "适用客户必须为正整数")
    private Long customerId;

    /** 检验类型：FIRST_ARTICLE 首件、LAST_ARTICLE 末件、PATROL 巡检、WAREHOUSE_IN 入库检、SHIPMENT 发货检。 */
    @Pattern(regexp = "^(FIRST_ARTICLE|LAST_ARTICLE|PATROL|WAREHOUSE_IN|SHIPMENT)$",
             message = "检验类型不合法")
    private String inspectionType;

    /** 方案状态：DRAFT 草稿、EFFECTIVE 生效、DISABLED 停用；为空时不限制生命周期状态。 */
    @Pattern(regexp = "^(DRAFT|EFFECTIVE|DISABLED)$", message = "方案状态不合法")
    private String planStatus;
}
