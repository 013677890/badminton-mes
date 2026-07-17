package com.badminton.mes.module.device.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设备联调记录创建请求。
 *
 * <p>分别收集通信链路、数据格式和综合联调结论。综合通过必须建立在前两项均成功之上；任一检查失败时
 * 必须提供问题说明。服务层会把最新综合结论同步到接入配置，失败结论还会关闭正式采集开关。
 */
@Data
public class DeviceCommissioningSaveReqVO {

    /** 本次联调针对的接入配置主键。 */
    @NotNull(message = "设备接入配置不能为空")
    @Positive(message = "设备接入配置必须为正整数")
    private Long accessConfigId;

    /** 实际联调时间，不得晚于服务器当前时间。 */
    @NotNull(message = "联调时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime testTime;

    /** 通信链路能否正常收发数据的检查结果。 */
    @NotNull(message = "通信结果不能为空")
    @Pattern(regexp = "^(SUCCESS|FAILED)$", message = "通信结果必须为 SUCCESS 或 FAILED")
    private String communicationResult;

    /** 报文字段、编码和结构是否符合约定的检查结果。 */
    @NotNull(message = "数据格式结果不能为空")
    @Pattern(regexp = "^(SUCCESS|FAILED)$", message = "数据格式结果必须为 SUCCESS 或 FAILED")
    private String dataFormatResult;

    /** 本次联调综合结论。 */
    @NotNull(message = "联调结果不能为空")
    @Pattern(regexp = "^(PASSED|FAILED)$", message = "联调结果必须为 PASSED 或 FAILED")
    private String testResult;

    /** 失败原因和排障线索；任一检查失败时必填。 */
    @Size(max = 500, message = "问题说明长度不能超过 500")
    private String issueDescription;

    /** 联调使用的样例报文，用于后续协议复核。 */
    @Size(max = 5000, message = "样例报文长度不能超过 5000")
    private String samplePayload;

    /**
     * 校验失败联调是否提供了可追踪的问题说明。
     *
     * @return 所有检查成功，或失败时问题说明非空则返回 {@code true}
     */
    @AssertTrue(message = "联调失败时必须填写问题说明")
    public boolean isIssueDescriptionValid() {
        boolean anyCheckFailed = "FAILED".equals(communicationResult)
                || "FAILED".equals(dataFormatResult)
                || "FAILED".equals(testResult);
        return !anyCheckFailed || (issueDescription != null && !issueDescription.isBlank());
    }

    /**
     * 校验综合通过结论与分项检查是否一致。
     *
     * @return 未声明通过，或通信和数据格式检查均成功时返回 {@code true}
     */
    @AssertTrue(message = "通信和数据格式检查成功后才能通过联调")
    public boolean isPassedResultConsistent() {
        return !"PASSED".equals(testResult)
                || ("SUCCESS".equals(communicationResult) && "SUCCESS".equals(dataFormatResult));
    }
}
