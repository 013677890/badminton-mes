package com.badminton.mes.module.production.controller.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 携带预期版本的产线修改请求。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductionLineUpdateReqVO extends ProductionLineSaveReqVO {

    /** 客户端读取时的版本 */
    @NotNull(message = "产线版本不能为空")
    @PositiveOrZero(message = "产线版本不能小于 0")
    private Integer version;
}
