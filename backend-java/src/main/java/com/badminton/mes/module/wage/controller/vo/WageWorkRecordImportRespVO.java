package com.badminton.mes.module.wage.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 报工快照导入结果。 */
@Data
@AllArgsConstructor
public class WageWorkRecordImportRespVO {
    /** 成功导入数量 */
    private Integer importedCount;
    /** 幂等跳过数量 */
    private Integer duplicateCount;
}
