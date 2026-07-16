package com.badminton.mes.module.report.service.impl;

import java.util.List;

import com.badminton.mes.module.report.dal.ReportQueryRepository;
import com.badminton.mes.module.report.service.DefectSourceProvider;
import com.badminton.mes.module.report.service.dto.DefectSourceBatch;
import com.badminton.mes.module.report.service.dto.ReportQueryCriteria;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * B 组生产报工不良来源适配器。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@Component
@Order(0)
public class SceneDefectSourceProvider implements DefectSourceProvider {

    private final ReportQueryRepository repository;

    public SceneDefectSourceProvider(ReportQueryRepository repository) {
        this.repository = repository;
    }

    @Override
    public DefectSourceBatch load(ReportQueryCriteria criteria, int limit) {
        return new DefectSourceBatch(repository.listSceneDefects(criteria, limit), List.of());
    }
}
