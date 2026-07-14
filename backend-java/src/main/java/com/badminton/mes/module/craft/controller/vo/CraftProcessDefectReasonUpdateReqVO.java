package com.badminton.mes.module.craft.controller.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工序不良原因修改请求 VO，携带客户端读取时的预期版本。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CraftProcessDefectReasonUpdateReqVO extends CraftProcessDefectReasonSaveReqVO {

    /** 客户端读取不良原因时获得的版本号 */
    @NotNull(message = "不良原因版本不能为空")
    @PositiveOrZero(message = "不良原因版本不能小于 0")
    private Integer version;
}
