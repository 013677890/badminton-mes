package com.badminton.mes.module.scene.controller.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 完工单草稿或驳回单修改请求。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@Data
public class SceneCompletionSaveReqVO {

    /** 完工数量 */
    @NotNull
    @Positive
    private Integer finishQuantity;
}
