package com.badminton.mes.module.scene.enums;

import lombok.Getter;

/** 生产任务状态。 @author 刘涵 */
@Getter
public enum SceneTaskStatusEnum {
    /** A 组派工兼容状态，对应 B 组待审核。 */
    PENDING(0),
    PENDING_AUDIT(0), AUDITED(1), RELEASED(2), IN_PRODUCTION(3),
    PAUSED(4), FINISHED(5), CLOSED(6), CANCELLED(7);
    private final Integer status;
    SceneTaskStatusEnum(Integer status) { this.status = status; }
}
