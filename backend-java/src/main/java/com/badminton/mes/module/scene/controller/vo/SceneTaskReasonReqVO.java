package com.badminton.mes.module.scene.controller.vo;
import jakarta.validation.constraints.*;
import lombok.Data;
/** 任务原因请求。 @author 刘涵 */
@Data public class SceneTaskReasonReqVO { @NotBlank @Size(max=255) private String reason; }
