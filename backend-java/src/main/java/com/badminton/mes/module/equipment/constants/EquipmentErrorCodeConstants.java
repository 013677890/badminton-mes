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

    /** 设备制造商不存在或已删除 */
    public static final ErrorCode EQUIPMENT_MANUFACTURER_NOT_EXISTS =
            new ErrorCode("A0402", "设备制造商不存在", "设备制造商不存在或已被删除，请刷新后重试");

    /** 设备制造商编码重复 */
    public static final ErrorCode EQUIPMENT_MANUFACTURER_CODE_DUPLICATE =
            new ErrorCode("A0506", "制造商编码已存在", "制造商编码重复，请更换制造商编码后重试");

    /** 设备制造商下存在设备，不允许删除 */
    public static final ErrorCode EQUIPMENT_MANUFACTURER_HAS_EQUIPMENT =
            new ErrorCode("A0440", "该制造商下存在设备，不允许删除", "请先删除或移动该制造商下的设备");

    /** 设备台账不存在或已删除 */
    public static final ErrorCode EQUIPMENT_LEDGER_NOT_EXISTS =
            new ErrorCode("A0402", "设备不存在", "设备台账不存在或已被删除，请刷新后重试");

    /** 设备编码重复 */
    public static final ErrorCode EQUIPMENT_LEDGER_CODE_DUPLICATE =
            new ErrorCode("A0506", "设备编码已存在", "设备编码重复，请更换设备编码后重试");

    /** 设备状态不允许当前操作 */
    public static final ErrorCode EQUIPMENT_STATUS_OPERATION_NOT_ALLOWED =
            new ErrorCode("A0440", "设备状态不允许当前操作", "请调整设备状态后再执行该操作");

    /** 设备故障原理不存在或已删除 */
    public static final ErrorCode EQUIPMENT_FAULT_PRINCIPLE_NOT_EXISTS =
            new ErrorCode("A0402", "故障原理不存在", "设备故障原理不存在或已被删除，请刷新后重试");

    /** 设备故障原理编码重复 */
    public static final ErrorCode EQUIPMENT_FAULT_PRINCIPLE_CODE_DUPLICATE =
            new ErrorCode("A0506", "故障编码已存在", "故障编码重复，请更换故障编码后重试");

    /** 设备类别下存在故障原理，不允许删除 */
    public static final ErrorCode EQUIPMENT_CATEGORY_HAS_FAULT_PRINCIPLE =
            new ErrorCode("A0440", "该类别下存在故障原理，不允许删除", "请先删除或移动该类别下的故障原理");

    /** 设备报修任务不存在或已删除 */
    public static final ErrorCode EQUIPMENT_REPAIR_ORDER_NOT_EXISTS =
            new ErrorCode("A0402", "报修任务不存在", "设备报修任务不存在或已被删除，请刷新后重试");

    /** 设备报修单号重复 */
    public static final ErrorCode EQUIPMENT_REPAIR_ORDER_NO_DUPLICATE =
            new ErrorCode("A0506", "报修单号已存在", "报修单号重复，请更换报修单号后重试");

    /** 设备下存在报修任务，不允许删除 */
    public static final ErrorCode EQUIPMENT_LEDGER_HAS_REPAIR_ORDER =
            new ErrorCode("A0440", "该设备存在报修任务，不允许删除", "请先删除或完成该设备关联的报修任务");

    /** 故障原理下存在报修任务，不允许删除 */
    public static final ErrorCode EQUIPMENT_FAULT_PRINCIPLE_HAS_REPAIR_ORDER =
            new ErrorCode("A0440", "该故障原理存在报修任务，不允许删除", "请先删除或变更该故障原理关联的报修任务");

    /** 报修状态流转不允许当前操作 */
    public static final ErrorCode EQUIPMENT_REPAIR_STATUS_OPERATION_NOT_ALLOWED =
            new ErrorCode("A0440", "报修状态不允许当前操作", "请调整报修状态后再执行该操作");

    private EquipmentErrorCodeConstants() {
    }
}
