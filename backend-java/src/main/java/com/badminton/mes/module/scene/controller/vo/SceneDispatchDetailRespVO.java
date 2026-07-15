package com.badminton.mes.module.scene.controller.vo;
import java.time.LocalDateTime;
import lombok.Data;
/** 派工工序明细响应。 @author 刘涵 */
@Data
public class SceneDispatchDetailRespVO {
    private Long id;private Long processId;private String processCode;private String processName;private Integer seq;
    private Boolean keyProcess;private Boolean inspect;private Boolean scanRequired;private Long sopId;
    private String sopCode;private String sopName;private String sopVersion;private Long stationId;private Long userId;
    private Long equipmentId;private Integer planQuantity;private Integer detailStatus;private Boolean paused;
    private String pauseReason;private LocalDateTime actualStartTime;private LocalDateTime actualEndTime;
}
