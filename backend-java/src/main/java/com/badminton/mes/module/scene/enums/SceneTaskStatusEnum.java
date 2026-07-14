package com.badminton.mes.module.scene.enums;

import lombok.Getter;

/** B 组现场生产任务状态。 */
@Getter
public enum SceneTaskStatusEnum {
    PENDING_AUDIT(0), AUDITED(1), RELEASED(2), IN_PRODUCTION(3),
    PAUSED(4), FINISHED(5), CLOSED(6), CANCELLED(7);

    private final Integer status;

    SceneTaskStatusEnum(Integer status) {
        this.status = status;
    }
}
