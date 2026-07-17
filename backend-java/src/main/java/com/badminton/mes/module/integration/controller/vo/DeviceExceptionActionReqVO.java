package com.badminton.mes.module.integration.controller.vo;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设备计数异常处理请求。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
public class DeviceExceptionActionReqVO {

    /** 人工忽略或修正处理时留下的审计说明，可为空。 */
    @Size(max = 255, message = "处理说明长度不能超过 255")
    private String remark;
}
