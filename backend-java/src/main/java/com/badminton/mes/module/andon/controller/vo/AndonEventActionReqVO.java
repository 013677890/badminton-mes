package com.badminton.mes.module.andon.controller.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 安灯异常确认、处理、转派、完成和关闭动作请求。 */
@Data
public class AndonEventActionReqVO {

    @Positive(message = "实际原因必须为正整数")
    private Long actualReasonId;

    @Positive(message = "目标处理人必须为正整数")
    private Long targetUserId;

    @Size(max = 32, message = "目标角色编码长度不能超过 32")
    private String targetRoleCode;

    @Size(max = 1000, message = "操作说明长度不能超过 1000")
    private String actionContent;

    @Size(max = 1000, message = "处理结果长度不能超过 1000")
    private String processingResult;

    @Min(value = 0, message = "影响时长不能小于 0")
    private Integer impactMinutes;

    @Min(value = 0, message = "影响数量不能小于 0")
    private Integer affectedQuantity;
}
