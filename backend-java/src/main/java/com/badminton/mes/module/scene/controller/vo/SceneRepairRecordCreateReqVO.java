package com.badminton.mes.module.scene.controller.vo;

import jakarta.validation.constraints.*;
import lombok.Data;

/** 提交返修记录请求。 @author 刘涵 */
@Data
public class SceneRepairRecordCreateReqVO {
    @NotNull @Positive private Integer quantity;
    @NotBlank @Size(max = 500) private String description;
}
