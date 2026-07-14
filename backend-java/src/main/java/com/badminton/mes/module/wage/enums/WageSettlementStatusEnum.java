package com.badminton.mes.module.wage.enums;

import java.util.Arrays;

import lombok.Getter;

/** 计件工资结算状态。 */
@Getter
public enum WageSettlementStatusEnum {
    /** 草稿 */
    DRAFT(0),
    /** 待审核 */
    PENDING(1),
    /** 已审核 */
    APPROVED(2),
    /** 已驳回 */
    REJECTED(3);

    private final Integer status;

    WageSettlementStatusEnum(Integer status) {
        this.status = status;
    }

    /**
     * 判断状态值是否合法。
     *
     * @param status 状态值
     * @return true 表示合法
     */
    public static boolean contains(Integer status) {
        return Arrays.stream(values()).anyMatch(item -> item.status.equals(status));
    }
}
