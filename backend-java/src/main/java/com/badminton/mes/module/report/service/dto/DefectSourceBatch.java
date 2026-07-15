package com.badminton.mes.module.report.service.dto;

import java.util.List;

/**
 * 单个不良来源提供的数据和降级警告。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
public record DefectSourceBatch(List<DefectSourceRecord> records, List<String> warnings) {

    public DefectSourceBatch {
        records = records == null ? List.of() : List.copyOf(records);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
