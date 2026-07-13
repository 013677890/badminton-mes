package com.badminton.mes.module.scene.controller.vo;

import jakarta.validation.constraints.*;
import lombok.Data;

/** 生产参数保存请求。 @author 刘涵 */
@Data
public class SceneProductionParameterSaveReqVO {
    @NotBlank @Size(max = 64) private String paramCode;
    @NotBlank @Size(max = 128) private String paramName;
    @NotBlank @Size(max = 255) private String paramValue;
    @NotNull @Min(1) @Max(4) private Integer valueType;
    @Positive private Long workshopId;
    @Positive private Long lineId;
    @Positive private Long productId;
    @Size(max = 255) private String remark;
    @NotBlank @Size(max = 255) private String changeReason;
}
