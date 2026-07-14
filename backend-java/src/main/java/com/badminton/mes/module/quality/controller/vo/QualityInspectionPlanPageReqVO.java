package com.badminton.mes.module.quality.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 检验标准方案分页请求。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QualityInspectionPlanPageReqVO extends PageParam {

    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    @Positive(message = "适用产品必须为正整数")
    private Long productId;

    @Positive(message = "适用客户必须为正整数")
    private Long customerId;

    @Pattern(regexp = "^(FIRST_ARTICLE|LAST_ARTICLE|PATROL|WAREHOUSE_IN|SHIPMENT)$",
             message = "检验类型不合法")
    private String inspectionType;

    @Pattern(regexp = "^(DRAFT|EFFECTIVE|DISABLED)$", message = "方案状态不合法")
    private String planStatus;
}
