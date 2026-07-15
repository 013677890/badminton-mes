package com.badminton.mes.module.scene.controller.vo;

import jakarta.validation.constraints.*;
import lombok.Data;

/** 提交返修复检请求。 @author 刘涵 */
@Data
public class SceneRepairRecheckReqVO {
    @NotBlank @Pattern(regexp = "RELEASED|CONTINUE_REPAIR|SCRAPPED") private String result;
    @NotNull @Positive private Integer quantity;
}
