package com.badminton.mes.module.scene.enums;

import lombok.Getter;

/** 工序作业状态。 @author 刘涵 */
@Getter
public enum SceneOperationStatusEnum {
    PENDING(0), IN_PROGRESS(1), COMPLETED(2), ABNORMAL(3);
    private final Integer status;
    SceneOperationStatusEnum(Integer status) { this.status = status; }
}
