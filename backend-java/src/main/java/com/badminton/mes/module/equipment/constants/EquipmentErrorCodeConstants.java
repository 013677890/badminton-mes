package com.badminton.mes.module.equipment.constants;

import com.badminton.mes.common.core.ErrorCode;

/**
 * 设备管理模块错误码，与业务代码就近维护。
 *
 * <p>编码复用《Java开发手册(黄山版)》附表 3 宏观错误码，
 * 业务细节由 message 承载：
 * A0402 无效的用户输入 / A0420 参数值超出范围 / A0440 用户操作异常 / A0506 用户重复请求。
 *
 * @author 角色C
 * @date 2026/07/09
 */
public final class EquipmentErrorCodeConstants {

    /** 设备类别不存在或已删除 */
    public static final ErrorCode EQUIPMENT_CATEGORY_NOT_EXISTS =
            new ErrorCode("A0402", "设备类别不存在", "设备类别不存在或已被删除，请刷新后重试");

    /** 设备类别编码重复 */
    public static final ErrorCode EQUIPMENT_CATEGORY_CODE_DUPLICATE =
            new ErrorCode("A0506", "类别编码已存在", "类别编码重复，请更换类别编码后重试");

    /** 设备类别存在下级分类，不允许删除 */
    public static final ErrorCode EQUIPMENT_CATEGORY_HAS_CHILDREN =
            new ErrorCode("A0440", "该类别存在下级分类，不允许删除", "请先删除或移动下级分类");

    /** 设备类别下存在设备，不允许删除 */
    public static final ErrorCode EQUIPMENT_CATEGORY_HAS_EQUIPMENT =
            new ErrorCode("A0440", "该类别下存在设备，不允许删除", "请先删除或移动该类别下的设备");

    /** 父级类别不存在 */
    public static final ErrorCode PARENT_CATEGORY_NOT_EXISTS =
            new ErrorCode("A0402", "父级类别不存在", "所选父级类别不可用，请重新选择");

    /** 不能将类别设置为自己的子类别 */
    public static final ErrorCode CATEGORY_CANNOT_BE_SELF_PARENT =
            new ErrorCode("A0420", "不能将类别设置为自己的子类别", "请选择其他父级类别");

    /** 设备类别存在循环引用 */
    public static final ErrorCode CATEGORY_CYCLIC_REFERENCE =
            new ErrorCode("A0420", "类别层级存在循环引用", "请调整父级类别，避免形成循环结构");

    private EquipmentErrorCodeConstants() {
    }
}
