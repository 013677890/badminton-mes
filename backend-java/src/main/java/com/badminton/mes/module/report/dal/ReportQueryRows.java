package com.badminton.mes.module.report.dal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报表只读 SQL 的内部投影，避免把跨模块 Entity 暴露到 report API。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
public final class ReportQueryRows {

    public record Aggregate(long planQuantity,
                            long inputQuantity,
                            long goodQuantity,
                            long defectQuantity,
                            long reworkQuantity,
                            long finishQuantity,
                            long occurrenceInputQuantity,
                            long reversalInputQuantity,
                            long occurrenceGoodQuantity,
                            long reversalGoodQuantity,
                            long occurrenceDefectQuantity,
                            long reversalDefectQuantity) {
    }

    public record ReportDetail(Long reportId,
                               String reportNo,
                               Long taskId,
                               String taskNo,
                               String workOrderNo,
                               Long productId,
                               String productName,
                               String batchNo,
                               Long workshopId,
                               String workshopName,
                               Long lineId,
                               String lineName,
                               Long processId,
                               String processName,
                               Integer recordType,
                               Long sourceReportId,
                               int inputQuantity,
                               int goodQuantity,
                               int defectQuantity,
                               int reworkQuantity,
                               String reverseReason,
                               LocalDateTime reportTime) {
    }

    public record RealtimeTask(Long taskId,
                               String taskNo,
                               String workOrderNo,
                               Long productId,
                               String productName,
                               String batchNo,
                               Long workshopId,
                               String workshopName,
                               Long lineId,
                               String lineName,
                               Integer planQuantity,
                               Integer inputQuantity,
                               Integer goodQuantity,
                               Integer defectQuantity,
                               Integer finishQuantity,
                               Integer taskStatus,
                               boolean abnormal,
                               LocalDateTime actualStartTime,
                               LocalDateTime updateTime) {
    }

    public record RealtimeSupport(long equipmentTotalCount,
                                  long runningEquipmentCount,
                                  long unavailableEquipmentCount,
                                  long openAndonCount,
                                  long criticalAndonCount) {
    }

    public record TraceTask(Long id,
                            String taskNo,
                            Long workOrderId,
                            String workOrderNo,
                            Long productId,
                            String productCode,
                            String productName,
                            String batchNo,
                            Long workshopId,
                            String workshopName,
                            Long lineId,
                            String lineName,
                            Integer planQuantity,
                            Integer inputQuantity,
                            Integer goodQuantity,
                            Integer defectQuantity,
                            Integer reworkQuantity,
                            Integer finishQuantity,
                            Integer taskStatus,
                            LocalDateTime actualStartTime,
                            LocalDateTime actualEndTime) {
    }

    public record TraceWorkOrder(Long id,
                                 String workOrderNo,
                                 String batchNo,
                                 Long productId,
                                 String productName,
                                 String spec,
                                 Integer planQuantity,
                                 Integer inputQuantity,
                                 Integer finishQuantity,
                                 Integer defectQuantity,
                                 Integer reworkQuantity,
                                 Integer orderStatus) {
    }

    public record TraceBarcode(Long id,
                               String barcodeValue,
                               Long barcodeTypeId,
                               Integer barcodeMode,
                               Long productId,
                               Long materialId,
                               String batchNo,
                               Integer barcodeStatus,
                               LocalDateTime createTime) {
    }

    public record TraceBarcodeUse(Long id,
                                  Long barcodeId,
                                  Long processId,
                                  Long userId,
                                  Long equipmentId,
                                  Integer useType,
                                  LocalDateTime businessTime) {
    }

    public record TraceProcessHistory(Long id,
                                      Long processId,
                                      String processCode,
                                      String processName,
                                      Integer actionType,
                                      Long operatorId,
                                      String actionReason,
                                      LocalDateTime operateTime) {
    }

    public record TraceMaterial(Long materialId,
                                String materialCode,
                                String materialName,
                                BigDecimal requireQuantity,
                                BigDecimal issuedQuantity) {
    }

    public record TraceOptionalSource(String sourceType,
                                      String sourceId,
                                      String summary,
                                      LocalDateTime eventTime) {
    }

    private ReportQueryRows() {
    }
}
