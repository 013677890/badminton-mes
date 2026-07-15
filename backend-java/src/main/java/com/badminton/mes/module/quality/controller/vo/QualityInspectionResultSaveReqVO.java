package com.badminton.mes.module.quality.controller.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 单个检验项目实测结果保存请求。
 *
 * <p>该请求更新建单时生成的结果快照，不直接提交检验单；值是否必填及判定是否完整仍受项目规则约束。
 */
@Data
public class QualityInspectionResultSaveReqVO {

    /** 检验结果快照主键，而非检验项目主键；不能为空且必须为正整数。 */
    @NotNull(message = "检验结果不能为空")
    @Positive(message = "检验结果必须为正整数")
    private Long resultId;

    /** 现场采集的实际测量值或文本值，最长 255 个字符。 */
    @Size(max = 255, message = "实测值长度不能超过 255")
    private String measuredValue;

    /** 项目级判定结果：PASS 合格、FAIL 不合格。 */
    @Pattern(regexp = "^(PASS|FAIL)$", message = "项目判定结果必须为 PASS 或 FAIL")
    private String judgmentResult;

    /** 项目判定为 FAIL 时的不良现象或偏差说明，最长 500 个字符。 */
    @Size(max = 500, message = "不良说明长度不能超过 500")
    private String defectDescription;
}
