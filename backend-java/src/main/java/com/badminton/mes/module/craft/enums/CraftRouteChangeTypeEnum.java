package com.badminton.mes.module.craft.enums;

import lombok.Getter;

/**
 * 工艺路线变更类型。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Getter
public enum CraftRouteChangeTypeEnum {

    /** 创建路线 */
    CREATE(1),

    /** 修改草稿 */
    UPDATE(2),

    /** 审核生效 */
    APPROVE(3),

    /** 停用路线 */
    DISABLE(4),

    /** 删除草稿 */
    DELETE(5),

    /** 创建新版本 */
    CREATE_VERSION(6);

    /** 数据库存储值 */
    private final Integer type;

    CraftRouteChangeTypeEnum(Integer type) {
        this.type = type;
    }
}
