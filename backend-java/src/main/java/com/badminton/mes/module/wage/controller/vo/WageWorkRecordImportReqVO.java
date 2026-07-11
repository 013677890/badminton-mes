package com.badminton.mes.module.wage.controller.vo;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 已审核报工快照批量导入请求。 */
@Data
public class WageWorkRecordImportReqVO {
    /** 每次最多导入 100 条 */
    @NotEmpty(message = "报工数据不能为空")
    @Size(max = 100, message = "单次最多导入 100 条报工")
    private List<@NotNull @Valid WageWorkRecordItemReqVO> records;
}
