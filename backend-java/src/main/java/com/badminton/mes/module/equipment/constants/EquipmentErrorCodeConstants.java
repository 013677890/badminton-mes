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

    /** 设备故障原理与设备类别不匹配 */
    public static final ErrorCode EQUIPMENT_FAULT_PRINCIPLE_CATEGORY_NOT_MATCH =
            new ErrorCode("A0420", "故障原理不适用于该设备类别", "请选择通用故障原理或该设备类别下的故障原理");

    /** 报修状态流转不允许当前操作 */
    public static final ErrorCode EQUIPMENT_REPAIR_STATUS_OPERATION_NOT_ALLOWED =
            new ErrorCode("A0440", "报修状态不允许当前操作", "请调整报修状态后再执行该操作");

    /** 设备保养计划不存在或已删除 */
    public static final ErrorCode EQUIPMENT_MAINTENANCE_PLAN_NOT_EXISTS =
            new ErrorCode("A0402", "保养计划不存在", "设备保养计划不存在或已被删除，请刷新后重试");

    /** 设备保养计划编码重复 */
    public static final ErrorCode EQUIPMENT_MAINTENANCE_PLAN_CODE_DUPLICATE =
            new ErrorCode("A0506", "保养计划编码已存在", "保养计划编码重复，请更换编码后重试");

    /** 保养计划存在保养任务记录 */
    public static final ErrorCode EQUIPMENT_MAINTENANCE_PLAN_HAS_ACTIVE_RECORD =
            new ErrorCode("A0440", "保养计划存在任务记录，不允许删除或更换设备", "历史保养记录必须与原计划和设备保持关联");

    /** 保养计划已停用 */
    public static final ErrorCode EQUIPMENT_MAINTENANCE_PLAN_DISABLED =
            new ErrorCode("A0440", "保养计划已停用", "请先启用保养计划后再创建保养任务");

    /** 设备保养记录不存在或已删除 */
    public static final ErrorCode EQUIPMENT_MAINTENANCE_RECORD_NOT_EXISTS =
            new ErrorCode("A0402", "保养记录不存在", "设备保养记录不存在或已被删除，请刷新后重试");

    /** 设备保养任务编号重复 */
    public static final ErrorCode EQUIPMENT_MAINTENANCE_RECORD_NO_DUPLICATE =
            new ErrorCode("A0506", "保养任务编号已存在", "保养任务编号重复，请更换编号后重试");

    /** 设备保养状态流转不允许当前操作 */
    public static final ErrorCode EQUIPMENT_MAINTENANCE_STATUS_OPERATION_NOT_ALLOWED =
            new ErrorCode("A0440", "保养任务状态不允许当前操作", "请按照保养任务状态流程执行操作");

    /** 完成保养任务时缺少保养结果 */
    public static final ErrorCode EQUIPMENT_MAINTENANCE_RESULT_REQUIRED =
            new ErrorCode("A0420", "完成保养任务必须填写保养结果", "请选择正常或异常并填写必要说明");

    /** 保养负责人或执行人不存在、已删除或已停用 */
    public static final ErrorCode EQUIPMENT_MAINTENANCE_USER_NOT_EXISTS =
            new ErrorCode("A0402", "保养负责人或执行人不可用", "请选择存在、未删除且已启用的系统用户");

    /** 保养任务实际执行时间不符合约束 */
    public static final ErrorCode EQUIPMENT_MAINTENANCE_TIME_INVALID =
            new ErrorCode("A0420", "保养任务时间不合法", "开始和完成时间不能晚于当前时间，完成时间不能早于开始时间");

    /** 已完成或已取消的保养任务不可修改或删除 */
    public static final ErrorCode EQUIPMENT_MAINTENANCE_TERMINAL_RECORD_IMMUTABLE =
            new ErrorCode("A0440", "终态保养任务不可修改或删除", "已完成或已取消的保养任务必须作为历史记录保留");

    /** 设备存在保养计划或保养任务记录 */
    public static final ErrorCode EQUIPMENT_LEDGER_HAS_MAINTENANCE =
            new ErrorCode("A0440", "该设备存在保养业务，不允许删除", "历史保养计划和记录必须与原设备保持关联");

    private EquipmentErrorCodeConstants() {
    }
}
