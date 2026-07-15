package com.badminton.mes.module.report.service.dto;

import java.time.LocalDateTime;

/**
 * B/C/返修来源统一转换后的不良事实。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
public record DefectSourceRecord(String sourceType,
                                 Long sourceId,
                                 Long sourceDetailId,
                                 String defectGroupNo,
                                 Long taskId,
                                 String taskNo,
                                 String workOrderNo,
                                 Long productId,
                                 String productName,
                                 String batchNo,
                                 Long workshopId,
                                 Long lineId,
                                 Long processId,
                                 String processName,
                                 String defectCode,
                                 String defectName,
                                 long occurrenceQuantity,
                                 long reversalQuantity,
                                 LocalDateTime detectedTime) {

    public long netQuantity() {
        return occurrenceQuantity - reversalQuantity;
    }
}
