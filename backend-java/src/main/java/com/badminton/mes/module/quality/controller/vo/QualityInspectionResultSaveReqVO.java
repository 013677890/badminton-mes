package com.badminton.mes.module.quality.controller.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 单个检验项目实测结果保存请求。 */
@Data
public class QualityInspectionResultSaveReqVO {

    @NotNull(message = "检验结果不能为空")
    @Positive(message = "检验结果必须为正整数")
    private Long resultId;

    @Size(max = 255, message = "实测值长度不能超过 255")
    private String measuredValue;

    @Pattern(regexp = "^(PASS|FAIL)$", message = "项目判定结果必须为 PASS 或 FAIL")
    private String judgmentResult;

    @Size(max = 500, message = "不良说明长度不能超过 500")
    private String defectDescription;
}
