package com.badminton.mes.module.device.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 设备计数异常处理请求。 */
@Data
public class DeviceCountExceptionResolveReqVO {

    @NotBlank(message = "处理状态不能为空")
    @Pattern(regexp = "^(RESOLVED|IGNORED)$", message = "处理状态必须为 RESOLVED 或 IGNORED")
    private String processingStatus;

    @NotBlank(message = "处理结果不能为空")
    @Size(max = 500, message = "处理结果长度不能超过 500")
    private String processingResult;
}
