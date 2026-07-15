package com.badminton.mes.module.scene.controller.vo;
import java.time.*;
import lombok.Data;
/** 生产任务响应。 @author 刘涵 */
@Data
public class SceneProductionTaskRespVO {
    private Long id;private String taskNo;private Long workOrderId;private String workOrderNo;
    private Long productId;private String productCode;private String productName;private String batchNo;
    private Long routingId;private String routingCode;private String routingVersion;
    private Long workshopId;private String workshopName;private Long lineId;private String lineName;private Long shiftId;
    private LocalDate planDate;private Integer planQuantity;private Integer inputQuantity;private Integer goodQuantity;
    private Integer defectQuantity;private Integer reworkQuantity;private Integer finishQuantity;
    private LocalDateTime planStartTime;private LocalDateTime planEndTime;private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;private Integer taskStatus;private String pauseReason;
}
