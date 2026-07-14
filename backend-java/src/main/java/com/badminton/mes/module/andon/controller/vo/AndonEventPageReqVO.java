package com.badminton.mes.module.andon.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 现场安灯异常分页请求。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AndonEventPageReqVO extends PageParam {

    @Size(max = 128, message = "关键字长度不能超过 128")
    private String keyword;
    @Positive(message = "安灯类型必须为正整数") private Long andonTypeId;
    @Positive(message = "产线必须为正整数") private Long productionLineId;
    @Positive(message = "设备必须为正整数") private Long equipmentId;
    @Positive(message = "发起人必须为正整数") private Long initiatedBy;
    @Positive(message = "处理人必须为正整数") private Long assignedUserId;
    @Size(max = 32, message = "处理角色编码长度不能超过 32")
    private String assignedRoleCode;
    @Pattern(regexp = "^(WEB|TABLET|MOBILE|SYSTEM)$", message = "来源渠道不合法")
    private String sourceChannel;
    @Pattern(regexp = "^(PENDING_CONFIRMATION|CONFIRMED|PROCESSING|WAITING_CLOSE|CLOSED)$",
            message = "异常状态不合法")
    private String eventStatus;
    @Pattern(regexp = "^(NORMAL|MAJOR|CRITICAL)$", message = "异常级别不合法")
    private String severity;
    @Pattern(regexp = "^(NORMAL|RESPONSE_OVERDUE|ESCALATED)$", message = "超时状态不合法")
    private String timeoutStatus;
}
