package com.badminton.mes.module.barcode.controller.vo;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 条码规则预览请求 VO：按提交的规则配置试算生成效果，不落库、不消耗真实流水。
 *
 * <p>直接携带配置而非规则 id，支持保存前在编辑界面预览。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeRulePreviewReqVO {

    /** 流水号位数 */
    @NotNull(message = "流水号位数不能为空")
    @Min(value = 1, message = "流水号位数最小值为 1")
    @Max(value = 9, message = "流水号位数最大值为 9")
    private Integer serialLength;

    /** 组成明细 */
    @NotEmpty(message = "规则组成明细不能为空")
    @Valid
    private List<BarcodeRuleItemSaveReqVO> items;

    /** 样例产品编码，组成含产品编码变量时必填 */
    @Size(max = 64, message = "样例产品编码长度不能超过 64")
    private String sampleProductCode;

    /** 样例产线编码，组成含产线编码变量时必填 */
    @Size(max = 64, message = "样例产线编码长度不能超过 64")
    private String sampleLineCode;
}
