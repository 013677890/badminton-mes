package com.badminton.mes.module.scene.controller.vo;
import java.time.LocalDateTime;import lombok.Data;
/** 产品当前生产状态响应。 @author 刘涵 */
@Data public class SceneProductStatusRespVO {
 private Long id;private String batchNo;private Long taskId;private Long productId;private Long currentProcessId;
 private String currentProcessName;private Integer batchStatus;private Boolean abnormal;private LocalDateTime updateTime;
}
