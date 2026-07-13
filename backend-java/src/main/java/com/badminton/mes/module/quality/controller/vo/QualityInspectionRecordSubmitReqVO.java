package com.badminton.mes.module.quality.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 质量检验单提交请求。 */
@Data
public class QualityInspectionRecordSubmitReqVO {

    @NotBlank(message = "检验结论不能为空")
    @Pattern(regexp = "^(PASS|CONCESSION|REWORK|SCRAP)$", message = "检验结论不合法")
    private String conclusion;

    @Size(max = 500, message = "不合格说明长度不能超过 500")
    private String nonconformanceDescription;

    @Size(max = 500, message = "处置意见长度不能超过 500")
    private String disposition;
}
