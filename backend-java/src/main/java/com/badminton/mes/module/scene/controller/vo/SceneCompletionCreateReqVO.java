package com.badminton.mes.module.scene.controller.vo;
import jakarta.validation.constraints.*;
import lombok.Data;
/** 从任务创建完工单请求。 @author 刘涵 */
@Data public class SceneCompletionCreateReqVO{
 @NotNull @Positive private Long taskId;
 @NotNull @Positive private Integer finishQuantity;
}
