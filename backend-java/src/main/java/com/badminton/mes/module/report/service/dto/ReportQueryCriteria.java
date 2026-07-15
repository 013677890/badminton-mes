package com.badminton.mes.module.report.service.dto;

import java.time.LocalDateTime;

/**
 * 报表 Repository 使用的稳定查询条件。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
public record ReportQueryCriteria(LocalDateTime startTime,
                                  LocalDateTime endTime,
                                  Long workshopId,
                                  Long lineId,
                                  Long productId,
                                  Long workOrderId,
                                  Long taskId,
                                  Long processId,
                                  Long shiftId,
                                  String batchNo,
                                  Integer status) {
}
