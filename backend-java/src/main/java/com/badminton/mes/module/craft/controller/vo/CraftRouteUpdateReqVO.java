package com.badminton.mes.module.craft.controller.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工艺路线修改请求 VO，携带客户端预期版本。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CraftRouteUpdateReqVO extends CraftRouteSaveReqVO {

    /** 客户端读取路线时获得的版本号 */
    @NotNull(message = "路线版本号不能为空")
    @PositiveOrZero(message = "路线版本号不能小于 0")
    private Integer version;
}
