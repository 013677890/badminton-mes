package com.badminton.mes.module.device.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设备计数上报请求。
 *
 * <p>承载设备侧一次不可变采集事实。服务端通过配置编码定位规则，通过设备编码校验绑定关系，并使用
 * “配置主键、采集时间、设备流水号”生成 SHA-256 幂等键。客户端重试必须保持这三个字段不变，
 * 否则系统会将其视为新的业务报文。
 */
@Data
public class DeviceCountReportReqVO {

    /** 接入配置编码，用于定位采集规则并对配置行加锁。 */
    @NotBlank(message = "接入配置编码不能为空")
    @Size(max = 32, message = "接入配置编码长度不能超过 32")
    private String configCode;

    /** 设备编码，必须与配置绑定的台账设备一致。 */
    @NotBlank(message = "设备编码不能为空")
    @Size(max = 32, message = "设备编码长度不能超过 32")
    private String equipmentCode;

    /** 设备侧采集时间；不得晚于服务器当前时间，并参与幂等身份和累计前序定位。 */
    @NotNull(message = "采集时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectedAt;

    /** 设备端流水号；用于区分同一采集点的不同报文并参与幂等身份。 */
    @NotBlank(message = "设备端流水号不能为空")
    @Size(max = 64, message = "设备端流水号长度不能超过 64")
    private String serialNumber;

    /** 原始计数值；具体解释为累计值或本次增量由接入配置决定。 */
    @NotNull(message = "计数值不能为空")
    @PositiveOrZero(message = "计数值不能为负数")
    private Long countValue;

    /** 采集瞬间的设备运行状态快照。 */
    @Pattern(regexp = "^(IDLE|RUNNING|STOPPED)$", message = "设备上报运行状态必须为 IDLE、RUNNING 或 STOPPED")
    private String runtimeStatus;

    /** 采集瞬间的设备故障状态或故障码。 */
    @Size(max = 64, message = "故障状态长度不能超过 64")
    private String faultStatus;

    /** 未解析的设备原始报文，用于审计和协议问题复现。 */
    @Size(max = 5000, message = "原始报文长度不能超过 5000")
    private String rawPayload;
}
