package com.badminton.mes.module.production.controller.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/** BOM 生效和停用请求。 */
@Data
public class BomActionReqVO {
    /** 客户端读取时的乐观锁版本 */
    @NotNull(message = "BOM 锁版本不能为空")
    @PositiveOrZero(message = "BOM 锁版本不能小于 0")
    private Integer lockVersion;
}
