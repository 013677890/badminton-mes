package com.badminton.mes.module.quality.controller.vo;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 检验项目实测结果批量保存请求。
 *
 * <p>一次请求更新同一草稿检验单中的结果快照；列表元素继续执行单项字段校验，重复或越权结果主键由服务层拒绝。
 */
@Data
public class QualityInspectionResultsSaveReqVO {

    /** 待保存的结果集合；不能为空且包含 1 至 100 项，每个元素均执行嵌套校验。 */
    @Valid
    @NotEmpty(message = "检验结果不能为空")
    @Size(max = 100, message = "单次最多保存 100 个检验结果")
    private List<QualityInspectionResultSaveReqVO> results;
}
