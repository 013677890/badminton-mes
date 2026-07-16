package com.badminton.mes.module.quality.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 质量检验单分页筛选请求。
 *
 * <p>所有业务条件均可选，可按检验类型、单据阶段、最终结论及生产来源组合查询；分页边界继承自
 * {@link PageParam}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QualityInspectionRecordPageReqVO extends PageParam {

    /** 检验单号或来源单号的模糊搜索关键字，最长 64 个字符。 */
    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    /** 检验类型：FIRST_ARTICLE 首件、LAST_ARTICLE 末件、PATROL 巡检、WAREHOUSE_IN 入库检、SHIPMENT 发货检。 */
    @Pattern(regexp = "^(FIRST_ARTICLE|LAST_ARTICLE|PATROL|WAREHOUSE_IN|SHIPMENT)$",
             message = "检验类型不合法")
    private String inspectionType;

    /** 检验单状态：DRAFT 草稿、SUBMITTED 已提交；为空时查询全部状态。 */
    @Pattern(regexp = "^(DRAFT|SUBMITTED)$", message = "检验单状态不合法")
    private String recordStatus;

    /** 最终结论：PASS 合格、CONCESSION 让步接收、REWORK 返工、SCRAP 报废。 */
    @Pattern(regexp = "^(PASS|CONCESSION|REWORK|SCRAP)$", message = "检验结论不合法")
    private String conclusion;

    /** 关联生产工单主键精确条件；填写时必须为正整数。 */
    @Positive(message = "生产工单必须为正整数")
    private Long workOrderId;

    /** 被检产品主键精确条件；填写时必须为正整数。 */
    @Positive(message = "产品必须为正整数")
    private Long productId;

    /** 产品批次号精确条件，最长 64 个字符。 */
    @Size(max = 64, message = "产品批次号长度不能超过 64")
    private String batchNo;
}
