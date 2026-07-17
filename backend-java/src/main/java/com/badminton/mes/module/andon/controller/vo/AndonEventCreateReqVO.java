package com.badminton.mes.module.andon.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 现场安灯异常发起请求。
 *
 * <p>描述异常类型、来源及可追溯的生产/设备/质量上下文。服务层会验证关联对象状态并交叉校验车间、产线、
 * 工单和批次的一致性，再根据类型处理模式及产线/全局配置确定初始状态、指派、时限、通知与灯控结果。
 */
@Data
public class AndonEventCreateReqVO {

    /** 安灯类型主键；类型必须存在且启用，并决定事件采用无需处理、自行处理或请求协助模式。 */
    @NotNull(message = "安灯类型不能为空")
    @Positive(message = "安灯类型必须为正整数")
    private Long andonTypeId;

    /** 发起时选择的预判原因主键；如填写，必须属于所选安灯类型且处于启用状态。 */
    @Positive(message = "异常原因必须为正整数")
    private Long reasonId;

    /** 事件来源：网页、平板、移动端或系统自动触发，用于来源追踪和统计。 */
    @NotBlank(message = "来源渠道不能为空")
    @Pattern(regexp = "^(WEB|TABLET|MOBILE|SYSTEM)$", message = "来源渠道不合法")
    private String sourceChannel;

    /** 异常级别：普通、重大或严重；未填写时由服务层采用普通级别。 */
    @Pattern(regexp = "^(NORMAL|MAJOR|CRITICAL)$", message = "异常级别不合法")
    private String severity;

    /** 事件所属车间主键，可由工单或设备关联信息校验、补齐。 */
    @Positive(message = "车间必须为正整数") private Long workshopId;
    /** 事件所属产线主键，用于优先匹配产线级处理配置，并可由设备或质量记录校验、补齐。 */
    @Positive(message = "产线必须为正整数") private Long productionLineId;
    /** 关联生产工单主键；工单须处于可接受的活动状态。 */
    @Positive(message = "生产工单必须为正整数") private Long workOrderId;
    /** 关联生产任务主键；当前创建流程保留该字段但暂不支持此类引用。 */
    @Positive(message = "生产任务必须为正整数") private Long productionTaskId;
    /** 关联工序主键；当前创建流程保留该字段但暂不支持此类引用。 */
    @Positive(message = "工序必须为正整数") private Long processId;
    /** 关联设备台账主键；设备须启用且未报废，启用灯控时还用于定位现场设备。 */
    @Positive(message = "设备必须为正整数") private Long equipmentId;
    /** 关联质量检验记录主键；记录须已提交，并可提供工单、产线和批次上下文。 */
    @Positive(message = "质量检验单必须为正整数") private Long qualityRecordId;

    /** 生产批次号；与工单或质量记录同时提供时必须保持一致。 */
    @Size(max = 64, message = "批次号长度不能超过 64")
    private String batchNo;

    /** 现场异常现象、位置及影响的文字描述，也是发起过程日志的初始内容。 */
    @NotBlank(message = "异常描述不能为空")
    @Size(max = 1000, message = "异常描述长度不能超过 1000")
    private String description;

    /** 现场照片、视频或文档地址的序列化文本，由客户端约定具体组织形式。 */
    @Size(max = 10000, message = "附件地址内容过长")
    private String attachmentUrls;
}
