package com.badminton.mes.module.scene.controller.vo;
import jakarta.validation.constraints.*;
import lombok.Data;
/** 完工审核请求。 @author 刘涵 */
@Data public class SceneCompletionAuditReqVO{
 @NotNull private Boolean approved;
 @Size(max=255) private String remark;
}
