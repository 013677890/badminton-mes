package com.badminton.mes.module.device.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 设备联调记录创建请求。 */
@Data
public class DeviceCommissioningSaveReqVO {

    @NotNull(message = "设备接入配置不能为空")
    @Positive(message = "设备接入配置必须为正整数")
    private Long accessConfigId;

    @NotNull(message = "联调时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime testTime;

    @NotNull(message = "通信结果不能为空")
    @Pattern(regexp = "^(SUCCESS|FAILED)$", message = "通信结果必须为 SUCCESS 或 FAILED")
    private String communicationResult;

    @NotNull(message = "数据格式结果不能为空")
    @Pattern(regexp = "^(SUCCESS|FAILED)$", message = "数据格式结果必须为 SUCCESS 或 FAILED")
    private String dataFormatResult;

    @NotNull(message = "联调结果不能为空")
    @Pattern(regexp = "^(PASSED|FAILED)$", message = "联调结果必须为 PASSED 或 FAILED")
    private String testResult;

    @Size(max = 500, message = "问题说明长度不能超过 500")
    private String issueDescription;

    @Size(max = 5000, message = "样例报文长度不能超过 5000")
    private String samplePayload;

    @AssertTrue(message = "联调失败时必须填写问题说明")
    public boolean isIssueDescriptionValid() {
        boolean anyCheckFailed = "FAILED".equals(communicationResult)
                || "FAILED".equals(dataFormatResult)
                || "FAILED".equals(testResult);
        return !anyCheckFailed || (issueDescription != null && !issueDescription.isBlank());
    }

    @AssertTrue(message = "通信和数据格式检查成功后才能通过联调")
    public boolean isPassedResultConsistent() {
        return !"PASSED".equals(testResult)
                || ("SUCCESS".equals(communicationResult) && "SUCCESS".equals(dataFormatResult));
    }
}
