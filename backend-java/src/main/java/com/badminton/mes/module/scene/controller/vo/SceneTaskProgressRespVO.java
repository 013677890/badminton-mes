package com.badminton.mes.module.scene.controller.vo;
import lombok.Data;
/** M2 任务阶段进度响应。 @author 刘涵 */
@Data public class SceneTaskProgressRespVO {
    private Long taskId;private Integer taskStatus;private Integer planQuantity;
    private Long operationTotal;private Long operationCompleted;private Long currentOperationId;private String currentProcessName;
}
