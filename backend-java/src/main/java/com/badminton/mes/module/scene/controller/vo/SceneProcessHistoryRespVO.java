package com.badminton.mes.module.scene.controller.vo;
import java.time.LocalDateTime;import lombok.Data;
/** 产品工序履历响应。 @author 刘涵 */
@Data public class SceneProcessHistoryRespVO {
 private Long id;private Long dispatchDetailId;private Long processId;private String processCode;private String processName;
 private Integer actionType;private Long operatorId;private String actionReason;private LocalDateTime operateTime;
}
