package com.badminton.mes.module.andon.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 现场安灯异常发起请求。 */
@Data
public class AndonEventCreateReqVO {

    @NotNull(message = "安灯类型不能为空")
    @Positive(message = "安灯类型必须为正整数")
    private Long andonTypeId;

    @Positive(message = "异常原因必须为正整数")
    private Long reasonId;

    @NotBlank(message = "来源渠道不能为空")
    @Pattern(regexp = "^(WEB|TABLET|MOBILE|SYSTEM)$", message = "来源渠道不合法")
    private String sourceChannel;

    @Pattern(regexp = "^(NORMAL|MAJOR|CRITICAL)$", message = "异常级别不合法")
    private String severity;

    @Positive(message = "车间必须为正整数") private Long workshopId;
    @Positive(message = "产线必须为正整数") private Long productionLineId;
    @Positive(message = "生产工单必须为正整数") private Long workOrderId;
    @Positive(message = "生产任务必须为正整数") private Long productionTaskId;
    @Positive(message = "工序必须为正整数") private Long processId;
    @Positive(message = "设备必须为正整数") private Long equipmentId;
    @Positive(message = "质量检验单必须为正整数") private Long qualityRecordId;

    @Size(max = 64, message = "批次号长度不能超过 64")
    private String batchNo;

    @NotBlank(message = "异常描述不能为空")
    @Size(max = 1000, message = "异常描述长度不能超过 1000")
    private String description;

    @Size(max = 10000, message = "附件地址内容过长")
    private String attachmentUrls;
}
