package com.badminton.mes.module.report.controller.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/**
 * 产量报表响应模型。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
public final class ProductionReportRespVO {

    /** 产量汇总。 */
    @Data
    public static class Summary {
        private long planQuantity;
        private long inputQuantity;
        private long goodQuantity;
        private long defectQuantity;
        private long reworkQuantity;
        private long finishQuantity;
        private long occurrenceInputQuantity;
        private long reversalInputQuantity;
        private long occurrenceGoodQuantity;
        private long reversalGoodQuantity;
        private long occurrenceDefectQuantity;
        private long reversalDefectQuantity;
        private BigDecimal completionRate;
        private BigDecimal defectRate;
        private List<String> warnings = List.of();
    }

    /** 报工净额与审计发生额明细。 */
    @Data
    public static class Detail {
        private Long reportId;
        private String reportNo;
        private Long taskId;
        private String taskNo;
        private String workOrderNo;
        private Long productId;
        private String productName;
        private String batchNo;
        private Long workshopId;
        private String workshopName;
        private Long lineId;
        private String lineName;
        private Long processId;
        private String processName;
        private Integer recordType;
        private Long sourceReportId;
        private int occurrenceInputQuantity;
        private int reversalInputQuantity;
        private int netInputQuantity;
        private int occurrenceGoodQuantity;
        private int reversalGoodQuantity;
        private int netGoodQuantity;
        private int occurrenceDefectQuantity;
        private int reversalDefectQuantity;
        private int netDefectQuantity;
        private int occurrenceReworkQuantity;
        private int reversalReworkQuantity;
        private int netReworkQuantity;
        private LocalDateTime reportTime;
    }

    private ProductionReportRespVO() {
    }
}
