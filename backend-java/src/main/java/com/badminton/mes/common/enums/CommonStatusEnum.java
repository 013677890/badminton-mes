package com.badminton.mes.common.enums;

import lombok.Getter;

/**
 * 通用启用/停用状态枚举，对应基础资料表的 status 字段(1 启用 0 停用)。
 *
 * <p>供各模块做档案可用性校验时统一取值，避免魔法数字散落业务代码。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
@Getter
public enum CommonStatusEnum {

    /** 停用：档案保留但不允许被新单据引用 */
    DISABLED(0, "停用"),

    /** 启用：档案正常可用 */
    ENABLED(1, "启用");

    /** 状态值，与数据库 status 字段取值一致 */
    private final Integer status;

    /** 状态描述 */
    private final String description;

    CommonStatusEnum(Integer status, String description) {
        this.status = status;
        this.description = description;
    }
}
