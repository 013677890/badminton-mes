package com.badminton.mes.module.integration.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设备累计计数写入请求。
 *
 * <p>countValue 的正数校验由 Service 执行，以便非法计数进入异常池而不是在 Web 层丢弃。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
public class DeviceCountWriteReqVO {

    /** 来源系统编码 */
    @NotBlank(message = "来源系统不能为空")
    @Size(max = 32, message = "来源系统长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "来源系统只能包含字母、数字、下划线和连字符")
    private String sourceSystem;

    /** 来源系统内唯一幂等键；服务端统一转为大写，因此大小写不敏感 */
    @NotBlank(message = "外部幂等键不能为空")
    @Size(max = 64, message = "外部幂等键长度不能超过 64")
    @Pattern(regexp = "^[A-Za-z0-9._/-]+$", message = "外部幂等键包含不支持的字符")
    private String externalKey;

    /** 设备编码 */
    @NotBlank(message = "设备编码不能为空")
    @Size(max = 32, message = "设备编码长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "设备编码只能包含字母、数字、下划线和连字符")
    private String equipmentCode;

    /** MES 派工单号 */
    @NotBlank(message = "派工单号不能为空")
    @Size(max = 32, message = "派工单号长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "派工单号只能包含字母、数字、下划线和连字符")
    private String dispatchNo;

    /** MES 工序编码 */
    @NotBlank(message = "工序编码不能为空")
    @Size(max = 32, message = "工序编码长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "工序编码只能包含字母、数字、下划线和连字符")
    private String processCode;

    /** 设备采集时间 */
    @NotNull(message = "采集时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectTime;

    /** 设备累计计数值 */
    @NotNull(message = "计数值不能为空")
    private Long countValue;
}
