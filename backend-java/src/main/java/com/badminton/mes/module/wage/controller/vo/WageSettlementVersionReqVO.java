package com.badminton.mes.module.wage.controller.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/** 仅携带结算预期版本的请求。 */
@Data
public class WageSettlementVersionReqVO {
    /** 客户端读取时的结算版本 */
    @NotNull(message = "结算版本不能为空")
    @PositiveOrZero(message = "结算版本不能小于 0")
    private Integer version;
}
