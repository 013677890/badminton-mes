package com.badminton.mes.module.scene.enums;

import lombok.Getter;

/** 工序派工状态。 @author 刘涵 */
@Getter
public enum SceneDispatchStatusEnum {
    PENDING_CONFIRM(0), CONFIRMED(1), IN_PROGRESS(2), COMPLETED(3), CANCELLED(4);
    private final Integer status;
    SceneDispatchStatusEnum(Integer status) { this.status = status; }
}
