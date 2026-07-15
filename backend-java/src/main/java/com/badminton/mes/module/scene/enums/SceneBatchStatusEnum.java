package com.badminton.mes.module.scene.enums;

import lombok.Getter;

/** 产品批次状态。 @author 刘涵 */
@Getter
public enum SceneBatchStatusEnum {
    IN_PROCESS(1), PENDING_INSPECTION(2), REWORKING(3), ISOLATED(4), FINISHED(5), SCRAPPED(6);
    private final Integer status;
    SceneBatchStatusEnum(Integer status) { this.status = status; }
}
