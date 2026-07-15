package com.badminton.mes.module.scene.controller.vo;

import jakarta.validation.constraints.*;
import lombok.Data;

/** 创建返修工单请求。 @author 刘涵 */
@Data
public class SceneRepairCreateReqVO {
    @NotNull private Long sourceReportId;
    @NotBlank private String batchNo;
    @NotNull @Positive private Integer defectQuantity;
    @NotNull @Positive private Integer repairQuantity;
    @NotBlank @Size(max = 255) private String reason;
}
