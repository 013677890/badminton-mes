package com.badminton.mes.module.quality.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 质量检验单分页请求。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QualityInspectionRecordPageReqVO extends PageParam {

    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    @Pattern(regexp = "^(FIRST_ARTICLE|LAST_ARTICLE|PATROL|WAREHOUSE_IN|SHIPMENT)$",
             message = "检验类型不合法")
    private String inspectionType;

    @Pattern(regexp = "^(DRAFT|SUBMITTED)$", message = "检验单状态不合法")
    private String recordStatus;

    @Pattern(regexp = "^(PASS|CONCESSION|REWORK|SCRAP)$", message = "检验结论不合法")
    private String conclusion;

    @Positive(message = "生产工单必须为正整数")
    private Long workOrderId;

    @Positive(message = "产品必须为正整数")
    private Long productId;

    @Size(max = 64, message = "产品批次号长度不能超过 64")
    private String batchNo;
}
