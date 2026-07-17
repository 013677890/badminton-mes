package com.badminton.mes.module.andon.controller.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 安灯事件生命周期动作的统一请求。
 *
 * <p>确认、开始处理、转派、完成、关闭和升级端点复用此结构，但服务层会按动作与事件当前状态读取并校验相应字段；
 * 例如转派/升级关注目标责任主体，完成动作关注实际原因、处理结果和影响数据。
 */
@Data
public class AndonEventActionReqVO {

    /** 核定后的实际原因主键，确认或完成时用于修正发起阶段的预判原因。 */
    @Positive(message = "实际原因必须为正整数")
    private Long actualReasonId;

    /** 转派或人工升级后的具体处理用户主键；必须指向有效用户。 */
    @Positive(message = "目标处理人必须为正整数")
    private Long targetUserId;

    /** 转派或人工升级后的处理角色编码；可与目标用户共同形成指派范围。 */
    @Size(max = 32, message = "目标角色编码长度不能超过 32")
    private String targetRoleCode;

    /** 本次动作说明；用于过程日志，转派时还承担记录转派理由的责任追溯用途。 */
    @Size(max = 1000, message = "操作说明长度不能超过 1000")
    private String actionContent;

    /** 异常处置结论，完成处理并进入待关闭状态时使用。 */
    @Size(max = 1000, message = "处理结果长度不能超过 1000")
    private String processingResult;

    /** 异常影响生产或业务的持续时长，单位为分钟，完成处理时登记。 */
    @Min(value = 0, message = "影响时长不能小于 0")
    private Integer impactMinutes;

    /** 受异常影响的产品或业务对象数量，完成处理时登记。 */
    @Min(value = 0, message = "影响数量不能小于 0")
    private Integer affectedQuantity;
}
