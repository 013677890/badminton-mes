package com.badminton.mes.module.integration.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

/**
 * 设备报工绑定保存请求。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
public class EquipmentBindingSaveReqVO {

    @NotBlank(message = "设备编码不能为空")
    @Size(max = 32, message = "设备编码长度不能超过 32")
    private String equipmentCode;

    @NotNull(message = "产线不能为空")
    private Long lineId;

    private Long processId;

    private Long defaultEmployeeId;

    @NotNull(message = "是否自动报工不能为空")
    private Boolean autoReport;

    @NotNull(message = "最大计数增量不能为空")
    @Positive(message = "最大计数增量必须大于 0")
    private Long maxIncrement;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能为 0 或 1")
    @Max(value = 1, message = "状态只能为 0 或 1")
    private Integer status;
}
