package com.badminton.mes.module.report.service.impl;

import java.util.List;

import com.badminton.mes.module.report.service.DefectSourceProvider;
import com.badminton.mes.module.report.service.dto.DefectSourceBatch;
import com.badminton.mes.module.report.service.dto.ReportQueryCriteria;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * C 组质量和 M5 返修表契约未落库时的显式降级适配器。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@Component
@Order(100)
public class UnavailableExternalDefectSourceProvider implements DefectSourceProvider {

    @Override
    public DefectSourceBatch load(ReportQueryCriteria criteria, int limit) {
        return new DefectSourceBatch(List.of(), List.of(
                "QUALITY_INSPECTION：C组质量不良表契约尚未落库，当前结果不含质检不良",
                "REPAIR_RECHECK：M5返修复检表尚未落库，当前结果不含返修复检不良"));
    }
}
