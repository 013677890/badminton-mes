package com.badminton.mes.module.report.service;

import com.badminton.mes.module.report.service.dto.DefectSourceBatch;
import com.badminton.mes.module.report.service.dto.ReportQueryCriteria;

/**
 * 不良来源适配器，B/C/返修数据必须转换为 report 自己的 DTO。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
public interface DefectSourceProvider {

    /** 按统一条件加载一个来源的不良事实和降级警告。 */
    DefectSourceBatch load(ReportQueryCriteria criteria, int limit);
}
