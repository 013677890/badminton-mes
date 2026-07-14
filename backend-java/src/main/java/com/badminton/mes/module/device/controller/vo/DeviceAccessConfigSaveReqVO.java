package com.badminton.mes.module.device.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 设备接入配置创建/修改请求。 */
@Data
public class DeviceAccessConfigSaveReqVO {

    @NotBlank(message = "接入配置编码不能为空")
    @Size(max = 32, message = "接入配置编码长度不能超过 32")
    @Pattern(regexp = "^(?!__DELETED_).+$", message = "接入配置编码不能使用系统保留前缀")
    private String configCode;

    @NotBlank(message = "接入配置名称不能为空")
    @Size(max = 128, message = "接入配置名称长度不能超过 128")
    private String configName;

    @NotNull(message = "设备台账不能为空")
    @Positive(message = "设备台账必须为正整数")
    private Long equipmentId;

    @NotBlank(message = "采集点编码不能为空")
    @Size(max = 64, message = "采集点编码长度不能超过 64")
    @Pattern(regexp = "^(?!__DELETED_).+$", message = "采集点编码不能使用系统保留前缀")
    private String collectionPointCode;

    @Positive(message = "关联工序必须为正整数")
    private Long processId;

    @Positive(message = "关联产线必须为正整数")
    private Long productionLineId;

    @Pattern(regexp = "^(CUMULATIVE|INCREMENTAL)$", message = "计数模式必须为 CUMULATIVE 或 INCREMENTAL")
    private String countMode;

    @Positive(message = "异常跳变阈值必须为正整数")
    private Long spikeThreshold;

    @Pattern(regexp = "^(AUTO|PENDING_CONFIRMATION|NONE)$",
             message = "报工模式必须为 AUTO、PENDING_CONFIRMATION 或 NONE")
    private String reportMode;

    @Min(value = 0, message = "正式采集状态只能为 0 或 1")
    @Max(value = 1, message = "正式采集状态只能为 0 或 1")
    private Integer enabledStatus;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
