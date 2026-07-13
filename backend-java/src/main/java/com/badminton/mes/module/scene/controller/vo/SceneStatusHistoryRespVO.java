package com.badminton.mes.module.scene.controller.vo;
import java.time.LocalDateTime;import lombok.Data;
/** 产品状态履历响应。 @author 刘涵 */
@Data public class SceneStatusHistoryRespVO {
 private Long id;private Integer fromStatus;private Integer toStatus;private Long processId;
 private String changeReason;private Long operatorId;private LocalDateTime operateTime;
}
