package com.badminton.mes.module.scene.controller.vo;
import jakarta.validation.constraints.*;
import lombok.Data;
/** 报工全额冲销请求。 @author 刘涵 */
@Data public class SceneWorkReportReverseReqVO{
 @NotBlank @Size(max=64) private String requestNo;
 @NotBlank @Size(max=255) private String reason;
}
