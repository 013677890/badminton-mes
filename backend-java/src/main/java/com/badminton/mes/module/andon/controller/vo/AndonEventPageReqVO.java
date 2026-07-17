package com.badminton.mes.module.andon.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 现场安灯异常分页查询请求。
 *
 * <p>在通用页码、每页数量基础上组合事件业务条件；未填写的条件不参与过滤，适用于待办、超时监控、
 * 设备事件追踪和个人发起/处理记录等列表场景。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AndonEventPageReqVO extends PageParam {

    /** 模糊搜索关键字，用于匹配事件编号、批次号或异常描述等可检索文本。 */
    @Size(max = 128, message = "关键字长度不能超过 128")
    private String keyword;
    /** 按安灯类型主键筛选，用于聚合同类异常及其处理规则。 */
    @Positive(message = "安灯类型必须为正整数") private Long andonTypeId;
    /** 按事件所属产线主键筛选。 */
    @Positive(message = "产线必须为正整数") private Long productionLineId;
    /** 按关联设备台账主键筛选设备异常。 */
    @Positive(message = "设备必须为正整数") private Long equipmentId;
    /** 按事件发起用户主键筛选。 */
    @Positive(message = "发起人必须为正整数") private Long initiatedBy;
    /** 按当前具体处理用户主键筛选个人待办或经办事件。 */
    @Positive(message = "处理人必须为正整数") private Long assignedUserId;
    /** 按当前指派角色编码筛选角色待办。 */
    @Size(max = 32, message = "处理角色编码长度不能超过 32")
    private String assignedRoleCode;
    /** 按发起渠道筛选：网页、平板、移动端或系统自动发起。 */
    @Pattern(regexp = "^(WEB|TABLET|MOBILE|SYSTEM)$", message = "来源渠道不合法")
    private String sourceChannel;
    /** 按事件生命周期状态筛选：待确认、已确认、处理中、待关闭或已关闭。 */
    @Pattern(regexp = "^(PENDING_CONFIRMATION|CONFIRMED|PROCESSING|WAITING_CLOSE|CLOSED)$",
            message = "异常状态不合法")
    private String eventStatus;
    /** 按异常级别筛选：普通、重大或严重。 */
    @Pattern(regexp = "^(NORMAL|MAJOR|CRITICAL)$", message = "异常级别不合法")
    private String severity;
    /** 按超时处理状态筛选：正常、响应超时或已升级。 */
    @Pattern(regexp = "^(NORMAL|RESPONSE_OVERDUE|ESCALATED)$", message = "超时状态不合法")
    private String timeoutStatus;
}
