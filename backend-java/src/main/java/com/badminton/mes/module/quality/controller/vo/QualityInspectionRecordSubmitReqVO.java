package com.badminton.mes.module.quality.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 质量检验单最终提交请求。
 *
 * <p>提交会将检验单从 DRAFT 转为 SUBMITTED 并记录检验人及完成时间。结论必须与逐项判定一致：存在
 * FAIL 项时不能选择 PASS，全部项目通过时只能选择 PASS；非合格结论还必须提供处置意见。
 */
@Data
public class QualityInspectionRecordSubmitReqVO {

    /** 最终结论：PASS 合格、CONCESSION 让步接收、REWORK 返工、SCRAP 报废；不能为空。 */
    @NotBlank(message = "检验结论不能为空")
    @Pattern(regexp = "^(PASS|CONCESSION|REWORK|SCRAP)$", message = "检验结论不合法")
    private String conclusion;

    /** 不符合事实及偏差说明，最长 500 个字符；存在 FAIL 项目时必须填写。 */
    @Size(max = 500, message = "不合格说明长度不能超过 500")
    private String nonconformanceDescription;

    /** 对不合格对象的处置意见，最长 500 个字符；结论非 PASS 时必须填写。 */
    @Size(max = 500, message = "处置意见长度不能超过 500")
    private String disposition;

    @PositiveOrZero(message = "不良数量不能为负数")
    private Integer defectQuantity;

    @Size(max = 64, message = "不良归并号长度不能超过 64")
    private String defectGroupNo;
}
