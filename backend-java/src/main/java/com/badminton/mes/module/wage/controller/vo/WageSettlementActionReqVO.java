package com.badminton.mes.module.wage.controller.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 结算提交、审核或驳回请求。 */
@Data
public class WageSettlementActionReqVO {
    /** 客户端读取时的结算版本 */
    @NotNull(message = "结算版本不能为空")
    @PositiveOrZero(message = "结算版本不能小于 0")
    private Integer version;
    /** 操作意见，驳回时必填 */
    @Size(max = 255, message = "操作意见长度不能超过 255")
    private String reason;
}
