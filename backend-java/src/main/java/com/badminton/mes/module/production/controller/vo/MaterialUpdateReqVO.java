package com.badminton.mes.module.production.controller.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 携带预期版本的物料修改请求。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialUpdateReqVO extends MaterialSaveReqVO {
    /** 客户端读取时的乐观锁版本 */
    @NotNull(message = "物料版本不能为空")
    @PositiveOrZero(message = "物料版本不能小于 0")
    private Integer version;
}
