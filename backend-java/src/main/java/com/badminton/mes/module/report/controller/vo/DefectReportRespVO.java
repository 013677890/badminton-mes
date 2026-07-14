package com.badminton.mes.module.report.controller.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/**
 * 不良报表响应模型。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
public final class DefectReportRespVO {

    /** 统一不良来源明细或综合归并行。 */
    @Data
    public static class Detail {
        private String sourceType;
        private Long sourceId;
        private Long sourceDetailId;
        private String defectGroupNo;
        private Long taskId;
        private String taskNo;
        private String workOrderNo;
        private Long productId;
        private String productName;
        private String batchNo;
        private Long workshopId;
        private Long lineId;
        private Long processId;
        private String processName;
        private String defectCode;
        private String defectName;
        private long occurrenceQuantity;
        private long reversalQuantity;
        private long netQuantity;
        private LocalDateTime detectedTime;
    }

    /** 不良聚合汇总。 */
    @Data
    public static class Summary {
        private long sceneDefectQuantity;
        private long qualityDefectQuantity;
        private long repairRecheckDefectQuantity;
        private long comprehensiveDefectQuantity;
        private long sceneOccurrenceQuantity;
        private long sceneReversalQuantity;
        private long sourceRecordCount;
        private long comprehensiveEventCount;
        private long mergedDuplicateCount;
        private long reportInputQuantity;
        private BigDecimal sceneDefectRate;
        private BigDecimal comprehensiveDefectRate;
        private List<String> warnings = List.of();
    }

    private DefectReportRespVO() {
    }
}
