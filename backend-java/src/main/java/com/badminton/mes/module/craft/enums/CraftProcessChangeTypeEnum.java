package com.badminton.mes.module.craft.enums;

import lombok.Getter;

/**
 * 工序变更类型。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Getter
public enum CraftProcessChangeTypeEnum {

    /** 创建工序 */
    CREATE(1),

    /** 修改工序档案或规则 */
    UPDATE(2),

    /** 启用或停用 */
    STATUS_CHANGE(3),

    /** 逻辑删除 */
    DELETE(4),

    /** SOP 绑定变更 */
    SOP_BINDING(5),

    /** 不良原因变更 */
    DEFECT_REASON_BINDING(6);

    /** 变更类型值 */
    private final Integer type;

    CraftProcessChangeTypeEnum(Integer type) {
        this.type = type;
    }
}
