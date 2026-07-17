package com.badminton.mes.module.scene.controller.vo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
/** 参数状态变更请求。 @author 刘涵 */
@Data
public class SceneParameterChangeReqVO {
    @NotBlank @Size(max = 255) private String reason;
}
