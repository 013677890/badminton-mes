package com.badminton.mes.module.scene.enums;

import lombok.Getter;

/**
 * 现场任务状态。
 *
 * @author Codex
 * @date 2026/07/13
 */
@Getter
public enum SceneTaskStatusEnum {

    /** 待执行 */
    PENDING(0),

    /** 执行中 */
    EXECUTING(1),

    /** 已完成 */
    COMPLETED(2),

    /** 已取消 */
    CANCELLED(3);

    private final Integer status;

    SceneTaskStatusEnum(Integer status) {
        this.status = status;
    }
}
