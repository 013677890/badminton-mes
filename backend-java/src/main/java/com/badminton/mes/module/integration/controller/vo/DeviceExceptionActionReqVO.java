package com.badminton.mes.module.integration.controller.vo;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设备计数异常处理请求。
 *
 * @author Codex
 * @date 2026/07/13
 */
@Data
public class DeviceExceptionActionReqVO {

    @Size(max = 255, message = "处理说明长度不能超过 255")
    private String remark;
}
