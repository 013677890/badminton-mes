package com.badminton.mes.module.production.controller.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 携带预期锁版本的 BOM 草稿修改请求。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BomUpdateReqVO extends BomSaveReqVO {
    /** 客户端读取时的乐观锁版本 */
    @NotNull(message = "BOM 锁版本不能为空")
    @PositiveOrZero(message = "BOM 锁版本不能小于 0")
    private Integer lockVersion;
}
