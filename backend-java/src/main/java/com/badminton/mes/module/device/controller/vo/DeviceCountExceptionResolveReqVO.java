package com.badminton.mes.module.device.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设备计数异常人工处理请求。
 *
 * <p>仅允许把待处理异常结束为已解决或已忽略。服务层通过悲观锁保证同一异常只能由一个并发请求
 * 完成首次处置，避免后提交者覆盖处理人、处理时间和结论。
 */
@Data
public class DeviceCountExceptionResolveReqVO {

    /** 异常终态：已解决或已忽略。 */
    @NotBlank(message = "处理状态不能为空")
    @Pattern(regexp = "^(RESOLVED|IGNORED)$", message = "处理状态必须为 RESOLVED 或 IGNORED")
    private String processingStatus;

    /** 人工核实、修正或忽略异常的具体结论。 */
    @NotBlank(message = "处理结果不能为空")
    @Size(max = 500, message = "处理结果长度不能超过 500")
    private String processingResult;
}
