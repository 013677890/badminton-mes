package com.badminton.mes.module.quality.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 质量检验单创建请求。 */
@Data
public class QualityInspectionRecordCreateReqVO {

    @NotNull(message = "检验方案不能为空")
    @Positive(message = "检验方案必须为正整数")
    private Long planId;

    @Positive(message = "生产工单必须为正整数")
    private Long workOrderId;

    @Positive(message = "来源单据必须为正整数")
    private Long sourceDocumentId;

    @Size(max = 64, message = "来源单号长度不能超过 64")
    private String sourceDocumentNo;

    @Positive(message = "产品必须为正整数")
    private Long productId;

    @Positive(message = "客户必须为正整数")
    private Long customerId;

    @Positive(message = "产线必须为正整数")
    private Long productionLineId;

    @NotBlank(message = "产品批次号不能为空")
    @Size(max = 64, message = "产品批次号长度不能超过 64")
    private String batchNo;

    @NotNull(message = "抽样数量不能为空")
    @Positive(message = "抽样数量必须为正整数")
    private Integer sampleQuantity;
}
