package com.badminton.mes.module.report.controller.vo;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/**
 * 实时生产响应模型。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
public final class RealtimeProductionRespVO {

    /** 实时生产总览。 */
    @Data
    public static class Overview {
        private long activeTaskCount;
        private long pausedTaskCount;
        private long abnormalBatchCount;
        private long planQuantity;
        private long inputQuantity;
        private long goodQuantity;
        private long defectQuantity;
        private long equipmentTotalCount;
        private long runningEquipmentCount;
        private long unavailableEquipmentCount;
        private long openAndonCount;
        private long criticalAndonCount;
        private LocalDateTime lastRefreshTime;
        private String dataStatus;
        private List<String> warnings = List.of();
    }

    /** 当前在制任务。 */
    @Data
    public static class Task {
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
        private Integer planQuantity;
        private Integer inputQuantity;
        private Integer goodQuantity;
        private Integer defectQuantity;
        private Integer finishQuantity;
        private Integer taskStatus;
        private boolean abnormal;
        private LocalDateTime actualStartTime;
        private LocalDateTime updateTime;
    }

    private RealtimeProductionRespVO() {
    }
}
