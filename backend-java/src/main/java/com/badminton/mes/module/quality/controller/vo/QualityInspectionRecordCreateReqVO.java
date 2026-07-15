package com.badminton.mes.module.quality.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 质量检验单草稿创建请求。
 *
 * <p>控制器另行接收检验类型。首件、末件和巡检属于生产类检验，必须关联生产工单；入库检和发货检
 * 必须提供来源单据主键、来源单号及产品。所选方案须已生效、到达生效日期且与检验类型和适用范围一致。
 */
@Data
public class QualityInspectionRecordCreateReqVO {

    /** 采用的检验方案版本主键；不能为空且必须为正整数，方案须处于当前可用状态。 */
    @NotNull(message = "检验方案不能为空")
    @Positive(message = "检验方案必须为正整数")
    private Long planId;

    /** 生产工单主键；首件、末件和巡检时必填，填写时必须为正整数。 */
    @Positive(message = "生产工单必须为正整数")
    private Long workOrderId;

    /** 入库检或发货检的来源单据主键；此两类检验必填，且必须为正整数。 */
    @Positive(message = "来源单据必须为正整数")
    private Long sourceDocumentId;

    /** 入库检或发货检的来源单号；此两类检验必填，最长 64 个字符。 */
    @Size(max = 64, message = "来源单号长度不能超过 64")
    private String sourceDocumentNo;

    /** 被检产品主键；入库检或发货检必填，生产类检验填写时须与工单产品一致。 */
    @Positive(message = "产品必须为正整数")
    private Long productId;

    /** 客户主键；填写时必须为正整数，并须满足工单来源及方案客户适用范围。 */
    @Positive(message = "客户必须为正整数")
    private Long customerId;

    /** 生产线主键；填写时必须为正整数，用于记录被检对象的生产归属。 */
    @Positive(message = "产线必须为正整数")
    private Long productionLineId;

    /** 被检产品批次号；不能为空、最长 64 个字符，生产类检验须与工单已有批次一致。 */
    @NotBlank(message = "产品批次号不能为空")
    @Size(max = 64, message = "产品批次号长度不能超过 64")
    private String batchNo;

    /** 检验单总体抽样数量；不能为空且必须大于 0。 */
    @NotNull(message = "抽样数量不能为空")
    @Positive(message = "抽样数量必须为正整数")
    private Integer sampleQuantity;
}
