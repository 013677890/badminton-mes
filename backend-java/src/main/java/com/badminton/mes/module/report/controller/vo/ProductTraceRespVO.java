package com.badminton.mes.module.report.controller.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/**
 * 产品批次完整追溯响应。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@Data
public class ProductTraceRespVO {

    private String dataCompleteness;
    private List<String> warnings = List.of();
    private Task task;
    private WorkOrder workOrder;
    private List<Barcode> barcodes = List.of();
    private List<BarcodeUse> barcodeUses = List.of();
    private List<ProcessHistory> processHistories = List.of();
    private List<WorkReport> workReports = List.of();
    private List<Material> materials = List.of();
    private List<OptionalSourceItem> packingDetails = List.of();
    private List<OptionalSourceItem> qualityDefects = List.of();
    private List<OptionalSourceItem> repairRecords = List.of();
    private List<OptionalSourceItem> equipmentStatuses = List.of();
    private List<OptionalSourceItem> andonExceptions = List.of();

    /** 生产任务快照。 */
    @Data
    public static class Task {
        private Long id;
        private String taskNo;
        private Long productId;
        private String productCode;
        private String productName;
        private String batchNo;
        private Long workshopId;
        private String workshopName;
        private Long lineId;
        private String lineName;
        private Integer planQuantity;
        private Integer inputQuantity;
        private Integer goodQuantity;
        private Integer defectQuantity;
        private Integer reworkQuantity;
        private Integer finishQuantity;
        private Integer taskStatus;
        private LocalDateTime actualStartTime;
        private LocalDateTime actualEndTime;
    }

    /** 上游工单信息。 */
    @Data
    public static class WorkOrder {
        private Long id;
        private String workOrderNo;
        private String batchNo;
        private Long productId;
        private String productName;
        private String spec;
        private Integer planQuantity;
        private Integer inputQuantity;
        private Integer finishQuantity;
        private Integer defectQuantity;
        private Integer reworkQuantity;
        private Integer orderStatus;
    }

    /** 条码实例。 */
    @Data
    public static class Barcode {
        private Long id;
        private String barcodeValue;
        private Long barcodeTypeId;
        private Integer barcodeMode;
        private Long productId;
        private Long materialId;
        private String batchNo;
        private Integer barcodeStatus;
        private LocalDateTime createTime;
    }

    /** 条码扫码使用记录。 */
    @Data
    public static class BarcodeUse {
        private Long id;
        private Long barcodeId;
        private Long processId;
        private Long userId;
        private Long equipmentId;
        private Integer useType;
        private LocalDateTime businessTime;
    }

    /** 工序履历。 */
    @Data
    public static class ProcessHistory {
        private Long id;
        private Long processId;
        private String processCode;
        private String processName;
        private Integer actionType;
        private Long operatorId;
        private String actionReason;
        private LocalDateTime operateTime;
    }

    /** 报工发生、冲销和净额记录。 */
    @Data
    public static class WorkReport {
        private Long id;
        private String reportNo;
        private Integer recordType;
        private Long sourceReportId;
        private Long processId;
        private int occurrenceInputQuantity;
        private int reversalInputQuantity;
        private int netInputQuantity;
        private int occurrenceGoodQuantity;
        private int reversalGoodQuantity;
        private int netGoodQuantity;
        private int occurrenceDefectQuantity;
        private int reversalDefectQuantity;
        private int netDefectQuantity;
        private String reverseReason;
        private LocalDateTime reportTime;
    }

    /** 工单物料需求；当前上游尚未提供实际消耗批次时 materialBatchNo 为空。 */
    @Data
    public static class Material {
        private Long materialId;
        private String materialCode;
        private String materialName;
        private BigDecimal requireQuantity;
        private BigDecimal issuedQuantity;
        private String materialBatchNo;
    }

    /** 跨组可选来源的稳定展示投影，禁止直接返回其他模块 Entity。 */
    @Data
    public static class OptionalSourceItem {
        private String sourceType;
        private String sourceId;
        private String summary;
        private LocalDateTime eventTime;
    }
}
