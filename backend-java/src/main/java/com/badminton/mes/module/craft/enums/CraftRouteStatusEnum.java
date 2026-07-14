package com.badminton.mes.module.craft.enums;

import lombok.Getter;

/**
 * 工艺路线状态。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Getter
public enum CraftRouteStatusEnum {

    /** 草稿：允许修改和删除 */
    DRAFT(0),

    /** 生效：可用于新生产任务 */
    EFFECTIVE(1),

    /** 停用：仅供历史追溯 */
    DISABLED(2);

    /** 数据库存储值 */
    private final Integer status;

    CraftRouteStatusEnum(Integer status) {
        this.status = status;
    }
}
