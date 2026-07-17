package com.badminton.mes.module.report.constants;

import com.badminton.mes.common.core.ErrorCode;

/**
 * 报表与追溯模块错误码。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
public final class ReportErrorCodeConstants {

    public static final ErrorCode TRACE_NOT_FOUND = error("A0402", "未找到可追溯的产品批次或条码");
    public static final ErrorCode QUERY_RANGE_INVALID = error("A0402", "报表查询时间范围不合法");
    public static final ErrorCode QUERY_RANGE_EXCEEDED = error("A0420", "报表查询时间范围超过一年");
    public static final ErrorCode EXPORT_RANGE_EXCEEDED = error("A0420", "同步导出时间范围不能超过31天");
    public static final ErrorCode EXPORT_ROWS_EXCEEDED = error("A0420", "同步导出数据超过10000行");
    public static final ErrorCode DEFECT_ROWS_EXCEEDED = error("A0420", "不良聚合明细超过10000行");

    private static ErrorCode error(String code, String message) {
        return new ErrorCode(code, message, message + "，请调整查询条件后重试");
    }

    private ReportErrorCodeConstants() {
    }
}
