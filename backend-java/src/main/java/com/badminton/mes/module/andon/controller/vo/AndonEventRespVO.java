package com.badminton.mes.module.andon.controller.vo;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 现场安灯异常聚合详情响应。
 *
 * <p>除事件基本信息外，还返回当前状态与责任指派、响应/升级期限、超时和灯控结果、生命周期时间点，
 * 以及详情查询所附带的过程日志与通知记录；分页查询中的两个明细列表为空。
 */
@Data
public class AndonEventRespVO {

    /** 事件数据库主键，用于详情、动作和日志接口定位同一事件。 */
    private Long id;
    /** 事件业务编号，作为跨通知、日志和现场沟通的可读追踪标识。 */
    private String eventNo;
    /** 安灯类型主键，决定处理模式、默认时限、通知规则和灯控策略。 */
    private Long andonTypeId;
    /** 安灯类型编码，便于客户端按稳定业务编码识别类型。 */
    private String andonTypeCode;
    /** 安灯类型名称，用于事件页面直接展示。 */
    private String andonTypeName;
    /** 发起时选择的预判原因主键，可为空，表示现场尚未归因。 */
    private Long reasonId;
    /** 确认或完成阶段核定的实际原因主键，用于区分初报判断与最终归因。 */
    private Long actualReasonId;
    /** 发起渠道：{@code WEB}、{@code TABLET}、{@code MOBILE} 或 {@code SYSTEM}。 */
    private String sourceChannel;
    /** 异常级别：{@code NORMAL}、{@code MAJOR} 或 {@code CRITICAL}。 */
    private String severity;
    /** 事件所属车间主键，可由工单或设备等可信业务对象反向补齐。 */
    private Long workshopId;
    /** 事件所属产线主键，用于匹配优先于全局规则的产线级处理配置。 */
    private Long productionLineId;
    /** 关联生产工单主键，用于确认事件发生时的生产上下文。 */
    private Long workOrderId;
    /** 关联生产任务主键；当前事件创建流程暂不支持该引用，字段为后续业务扩展保留。 */
    private Long productionTaskId;
    /** 关联工序主键；当前事件创建流程暂不支持该引用，字段为后续业务扩展保留。 */
    private Long processId;
    /** 关联设备台账主键，也是启用灯控时定位现场设备的依据。 */
    private Long equipmentId;
    /** 关联质量检验记录主键，用于质量异常追溯并补齐工单、产线和批次信息。 */
    private Long qualityRecordId;
    /** 事件关联批次号，可由工单或质量记录校验并补齐。 */
    private String batchNo;
    /** 发起人提交的异常现象与现场情况描述。 */
    private String description;
    /** 附件地址的序列化文本，用于承载现场照片、视频或文档引用。 */
    private String attachmentUrls;
    /** 生命周期状态：待确认、已确认、处理中、待关闭或已关闭。 */
    private String eventStatus;
    /** 当前具体处理用户主键；用户与角色指派可按配置同时存在。 */
    private Long assignedUserId;
    /** 当前处理角色编码，使该角色成员可按权限规则处理事件。 */
    private String assignedRoleCode;

    /** 响应截止时间；待确认事件超过该时间后可被标记为响应超时，确认或闭环后清空。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime responseDeadline;

    /** 升级截止时间；到期后系统可改派升级责任主体并将超时状态置为已升级。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime escalationDeadline;

    /** 超时状态：{@code NORMAL}、{@code RESPONSE_OVERDUE} 或 {@code ESCALATED}。 */
    private String timeoutStatus;
    /** 灯控状态：无需控制、已开启、已关闭或控制失败，用于区分业务闭环与设备执行结果。 */
    private String lightStatus;
    /** 灯控执行说明，记录模拟或实际开关灯结果及失败原因。 */
    private String lightMessage;
    /** 处理完成时填写的处置结论，是进入待关闭状态所需的闭环信息。 */
    private String processingResult;
    /** 异常造成的影响时长，单位为分钟，允许为零。 */
    private Integer impactMinutes;
    /** 异常影响的产品或业务对象数量，允许为零。 */
    private Integer affectedQuantity;
    /** 事件发起用户主键，用于责任追溯和按发起人筛选。 */
    private Long initiatedBy;
    /** 确认事件的用户主键；无需处理类型可由系统按发起人自动闭环。 */
    private Long confirmedBy;

    /** 事件确认时间，按 {@code yyyy-MM-dd HH:mm:ss} 输出。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmedAt;

    /** 提交处理结果的用户主键。 */
    private Long completedBy;

    /** 处理完成并进入待关闭状态的时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    /** 最终关闭事件的用户主键，常规流程要求管理角色执行关闭。 */
    private Long closedBy;

    /** 事件正式闭环时间；该时间写入后事件状态为已关闭。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closedAt;

    /** 事件记录创建时间，按统一秒级格式输出。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 事件最后更新时间，状态、指派、超时或灯控结果变化时更新。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 按发生顺序排列的过程日志；仅事件详情查询装载。 */
    private List<AndonProcessLogRespVO> processLogs;
    /** 按发生顺序排列的通知记录；仅事件详情查询装载。 */
    private List<AndonNotificationRecordRespVO> notificationRecords;
}
