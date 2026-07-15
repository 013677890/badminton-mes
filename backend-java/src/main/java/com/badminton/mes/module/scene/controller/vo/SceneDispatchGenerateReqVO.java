package com.badminton.mes.module.scene.controller.vo;
import jakarta.validation.constraints.*;
import lombok.Data;
/** 派工生成请求。 @author 刘涵 */
@Data public class SceneDispatchGenerateReqVO { @NotNull @Positive private Long taskId; }
