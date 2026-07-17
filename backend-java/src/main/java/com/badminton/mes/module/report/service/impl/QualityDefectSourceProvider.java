package com.badminton.mes.module.report.service.impl;

import java.util.List;

import com.badminton.mes.module.report.dal.ReportQueryRepository;
import com.badminton.mes.module.report.service.DefectSourceProvider;
import com.badminton.mes.module.report.service.dto.DefectSourceBatch;
import com.badminton.mes.module.report.service.dto.ReportQueryCriteria;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * C 组质量检验不良来源适配器。
 *
 * @author 刘涵
 * @date 2026/07/14
 */
@Component
@Order(10)
public class QualityDefectSourceProvider implements DefectSourceProvider {

    private final ReportQueryRepository repository;

    public QualityDefectSourceProvider(ReportQueryRepository repository) {
        this.repository = repository;
    }

    @Override
    public DefectSourceBatch load(ReportQueryCriteria criteria, int limit) {
        return new DefectSourceBatch(repository.listQualityDefects(criteria, limit), List.of());
    }
}
