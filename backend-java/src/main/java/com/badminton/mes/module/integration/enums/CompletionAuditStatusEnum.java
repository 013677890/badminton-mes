package com.badminton.mes.module.integration.enums;

import lombok.Getter;

/**
 * 生产完工单审核状态，对应 prod_completion_order.audit_status 字段。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Getter
public enum CompletionAuditStatusEnum {

    /** 待审核 */
    PENDING(0, "待审核"),

    /** 已审核，可对外读取 */
    APPROVED(1, "已审核"),

    /** 已作废 */
    VOIDED(2, "已作废");

    /** 数据库存储值 */
    private final Integer status;

    /** 状态描述 */
    private final String description;

    CompletionAuditStatusEnum(Integer status, String description) {
        this.status = status;
        this.description = description;
    }
}
