package com.badminton.mes.module.scene.enums;

import lombok.Getter;

/**
 * 现场报工审核状态。
 *
 * @author Codex
 * @date 2026/07/13
 */
@Getter
public enum WorkReportAuditStatusEnum {

    /** 待确认 */
    PENDING(0),

    /** 已审核 */
    APPROVED(1),

    /** 已驳回 */
    REJECTED(2);

    private final Integer status;

    WorkReportAuditStatusEnum(Integer status) {
        this.status = status;
    }
}
