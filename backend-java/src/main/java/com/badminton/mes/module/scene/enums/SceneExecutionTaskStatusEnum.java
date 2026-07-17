package com.badminton.mes.module.scene.enums;

import lombok.Getter;

/** 派工下发、报工与完工闭环的执行任务状态。 */
@Getter
public enum SceneExecutionTaskStatusEnum {
    PENDING(0),
    EXECUTING(1),
    COMPLETED(2),
    CANCELLED(3);

    private final Integer status;

    SceneExecutionTaskStatusEnum(Integer status) {
        this.status = status;
    }
}
