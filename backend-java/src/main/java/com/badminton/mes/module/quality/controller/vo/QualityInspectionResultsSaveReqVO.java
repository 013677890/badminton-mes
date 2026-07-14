package com.badminton.mes.module.quality.controller.vo;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 检验项目实测结果批量保存请求。 */
@Data
public class QualityInspectionResultsSaveReqVO {

    @Valid
    @NotEmpty(message = "检验结果不能为空")
    @Size(max = 100, message = "单次最多保存 100 个检验结果")
    private List<QualityInspectionResultSaveReqVO> results;
}
