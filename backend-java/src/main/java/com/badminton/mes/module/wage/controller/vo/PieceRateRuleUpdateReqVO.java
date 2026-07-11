package com.badminton.mes.module.wage.controller.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 携带预期版本的计件规则修改请求。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PieceRateRuleUpdateReqVO extends PieceRateRuleSaveReqVO {
    /** 客户端读取时的规则版本 */
    @NotNull(message = "规则版本不能为空")
    @PositiveOrZero(message = "规则版本不能小于 0")
    private Integer version;
}
