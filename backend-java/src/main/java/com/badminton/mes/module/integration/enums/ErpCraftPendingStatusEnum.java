package com.badminton.mes.module.integration.enums;

import lombok.Getter;

/**
 * ERP 工艺待确认数据状态。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Getter
public enum ErpCraftPendingStatusEnum {

    /** 待确认：校验通过，等待工艺工程师确认 */
    PENDING(0),

    /** 已确认：已生成 MES 工艺路线草稿 */
    CONFIRMED(1),

    /** 校验异常：产品或工序校验未通过，可重新同步 */
    FAILED(2),

    /** 已驳回：工艺工程师明确不采用，可重新同步覆盖 */
    REJECTED(3);

    /** 数据库存储值 */
    private final Integer status;

    ErpCraftPendingStatusEnum(Integer status) {
        this.status = status;
    }
}
