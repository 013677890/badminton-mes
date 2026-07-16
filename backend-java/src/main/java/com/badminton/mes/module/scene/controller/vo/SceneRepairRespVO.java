package com.badminton.mes.module.scene.controller.vo;

import java.time.LocalDateTime;
import lombok.Data;

/** 返修工单响应。 @author 刘涵 */
@Data
public class SceneRepairRespVO {
    private Long id; private String repairNo; private Long sourceReportId; private Long taskId;
    private String batchNo; private Integer defectQuantity; private Integer repairQuantity;
    private String status; private String reason; private Long assigneeId;
    private String recheckResult; private Integer recheckQuantity;
    private LocalDateTime createdTime; private LocalDateTime updatedTime;
}
