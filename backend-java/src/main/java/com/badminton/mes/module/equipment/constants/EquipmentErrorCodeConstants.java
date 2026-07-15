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

    /** 查询、修改、停用或删除类别时，目标主键不存在或记录已被逻辑删除时触发。 */
    public static final ErrorCode EQUIPMENT_CATEGORY_NOT_EXISTS =
            new ErrorCode("A0402", "设备类别不存在", "设备类别不存在或已被删除，请刷新后重试");

    /** 新增类别或修改类别编码时，编码已被其他有效类别占用时触发。 */
    public static final ErrorCode EQUIPMENT_CATEGORY_CODE_DUPLICATE =
            new ErrorCode("A0506", "类别编码已存在", "类别编码重复，请更换类别编码后重试");

    /** 删除类别前发现其仍有未逻辑删除的直接子类别，层级引用尚未解除时触发。 */
    public static final ErrorCode EQUIPMENT_CATEGORY_HAS_CHILDREN =
            new ErrorCode("A0440", "该类别存在下级分类，不允许删除", "请先删除或移动下级分类");

    /** 删除类别前发现仍有有效设备台账引用该类别，设备尚未迁移或删除时触发。 */
    public static final ErrorCode EQUIPMENT_CATEGORY_HAS_EQUIPMENT =
            new ErrorCode("A0440", "该类别下存在设备，不允许删除", "请先删除或移动该类别下的设备");

    /** 停用或删除类别前发现启用工序仍将其作为所需设备类别时触发。 */
    public static final ErrorCode EQUIPMENT_CATEGORY_REFERENCED_BY_PROCESS =
            new ErrorCode("A0440", "设备类别仍被启用工序引用", "请先调整或停用引用该类别的工序");

    /** 停用或删除类别前发现已生效工艺路线版本仍引用该类别时触发。 */
    public static final ErrorCode EQUIPMENT_CATEGORY_REFERENCED_BY_ROUTE =
            new ErrorCode("A0440", "设备类别仍被生效工艺路线引用", "请先在工艺路线新版本中替换该设备类别");

    /** 新增或调整类别层级时，所选父类别不存在、已删除或不可用时触发。 */
    public static final ErrorCode PARENT_CATEGORY_NOT_EXISTS =
            new ErrorCode("A0402", "父级类别不存在", "所选父级类别不可用，请重新选择");

    /** 修改父级关系时将当前类别自身选为父节点，形成直接自引用时触发。 */
    public static final ErrorCode CATEGORY_CANNOT_BE_SELF_PARENT =
            new ErrorCode("A0420", "不能将类别设置为自己的子类别", "请选择其他父级类别");

    /** 修改父级关系时，新父节点位于当前类别后代链中，将形成间接循环时触发。 */
    public static final ErrorCode CATEGORY_CYCLIC_REFERENCE =
            new ErrorCode("A0420", "类别层级存在循环引用", "请调整父级类别，避免形成循环结构");

    /** 查询、修改或删除制造商时，目标主键不存在或档案已被逻辑删除时触发。 */
    public static final ErrorCode EQUIPMENT_MANUFACTURER_NOT_EXISTS =
            new ErrorCode("A0402", "设备制造商不存在", "设备制造商不存在或已被删除，请刷新后重试");

    /** 新增制造商或修改制造商编码时，编码已被其他有效档案占用时触发。 */
    public static final ErrorCode EQUIPMENT_MANUFACTURER_CODE_DUPLICATE =
            new ErrorCode("A0506", "制造商编码已存在", "制造商编码重复，请更换制造商编码后重试");

    /** 删除制造商前发现仍有有效设备台账引用该厂商，设备尚未迁移时触发。 */
    public static final ErrorCode EQUIPMENT_MANUFACTURER_HAS_EQUIPMENT =
            new ErrorCode("A0440", "该制造商下存在设备，不允许删除", "请先删除或移动该制造商下的设备");

    /** 查询、修改、状态操作或删除设备时，目标台账不存在或已逻辑删除时触发。 */
    public static final ErrorCode EQUIPMENT_LEDGER_NOT_EXISTS =
            new ErrorCode("A0402", "设备不存在", "设备台账不存在或已被删除，请刷新后重试");

    /** 新增设备或修改设备编码时，编码已被其他有效台账占用时触发。 */
    public static final ErrorCode EQUIPMENT_LEDGER_CODE_DUPLICATE =
            new ErrorCode("A0506", "设备编码已存在", "设备编码重复，请更换设备编码后重试");

    /** 设备当前运行状态不满足报修、保养、删除或其他状态迁移前置条件时触发。 */
    public static final ErrorCode EQUIPMENT_STATUS_OPERATION_NOT_ALLOWED =
            new ErrorCode("A0440", "设备状态不允许当前操作", "请调整设备状态后再执行该操作");

    /** 查询、修改、删除或报修引用故障原理时，目标不存在或已逻辑删除时触发。 */
    public static final ErrorCode EQUIPMENT_FAULT_PRINCIPLE_NOT_EXISTS =
            new ErrorCode("A0402", "故障原理不存在", "设备故障原理不存在或已被删除，请刷新后重试");

    /** 新增故障原理或修改故障编码时，编码已被其他有效字典项占用时触发。 */
    public static final ErrorCode EQUIPMENT_FAULT_PRINCIPLE_CODE_DUPLICATE =
            new ErrorCode("A0506", "故障编码已存在", "故障编码重复，请更换故障编码后重试");

    /** 删除设备类别前发现仍有有效故障原理归属该类别时触发。 */
    public static final ErrorCode EQUIPMENT_CATEGORY_HAS_FAULT_PRINCIPLE =
            new ErrorCode("A0440", "该类别下存在故障原理，不允许删除", "请先删除或移动该类别下的故障原理");

    /** 查询、修改或推进报修任务时，目标任务不存在或已逻辑删除时触发。 */
    public static final ErrorCode EQUIPMENT_REPAIR_ORDER_NOT_EXISTS =
            new ErrorCode("A0402", "报修任务不存在", "设备报修任务不存在或已被删除，请刷新后重试");

    /** 新增报修任务或修改报修单号时，单号已被其他有效任务占用时触发。 */
    public static final ErrorCode EQUIPMENT_REPAIR_ORDER_NO_DUPLICATE =
            new ErrorCode("A0506", "报修单号已存在", "报修单号重复，请更换报修单号后重试");

    /** 删除设备台账前发现仍有关联的有效报修任务或维修历史时触发。 */
    public static final ErrorCode EQUIPMENT_LEDGER_HAS_REPAIR_ORDER =
            new ErrorCode("A0440", "该设备存在报修任务，不允许删除", "请先删除或完成该设备关联的报修任务");

    /** 删除故障原理前发现仍有有效报修任务引用该字典项时触发。 */
    public static final ErrorCode EQUIPMENT_FAULT_PRINCIPLE_HAS_REPAIR_ORDER =
            new ErrorCode("A0440", "该故障原理存在报修任务，不允许删除", "请先删除或变更该故障原理关联的报修任务");

    /** 创建或修改报修任务时，所选故障原理既非通用项也不属于该设备类别时触发。 */
    public static final ErrorCode EQUIPMENT_FAULT_PRINCIPLE_CATEGORY_NOT_MATCH =
            new ErrorCode("A0420", "故障原理不适用于该设备类别", "请选择通用故障原理或该设备类别下的故障原理");

    /** 派工、开工、完工、取消或修改报修时，当前状态不允许目标操作或迁移时触发。 */
    public static final ErrorCode EQUIPMENT_REPAIR_STATUS_OPERATION_NOT_ALLOWED =
            new ErrorCode("A0440", "报修状态不允许当前操作", "请调整报修状态后再执行该操作");

    /** 查询、修改、删除计划或创建任务时，目标保养计划不存在或已逻辑删除时触发。 */
    public static final ErrorCode EQUIPMENT_MAINTENANCE_PLAN_NOT_EXISTS =
            new ErrorCode("A0402", "保养计划不存在", "设备保养计划不存在或已被删除，请刷新后重试");

    /** 新增保养计划或修改计划编码时，编码已被其他有效计划占用时触发。 */
    public static final ErrorCode EQUIPMENT_MAINTENANCE_PLAN_CODE_DUPLICATE =
            new ErrorCode("A0506", "保养计划编码已存在", "保养计划编码重复，请更换编码后重试");

    /** 删除计划或更换绑定设备时，计划下仍存在有效保养任务记录时触发。 */
    public static final ErrorCode EQUIPMENT_MAINTENANCE_PLAN_HAS_ACTIVE_RECORD =
            new ErrorCode("A0440", "保养计划存在任务记录，不允许删除或更换设备", "历史保养记录必须与原计划和设备保持关联");

    /** 根据保养计划创建新任务时，计划启停状态为停用时触发。 */
    public static final ErrorCode EQUIPMENT_MAINTENANCE_PLAN_DISABLED =
            new ErrorCode("A0440", "保养计划已停用", "请先启用保养计划后再创建保养任务");

    /** 查询、修改、删除或推进保养任务时，目标记录不存在或已逻辑删除时触发。 */
    public static final ErrorCode EQUIPMENT_MAINTENANCE_RECORD_NOT_EXISTS =
            new ErrorCode("A0402", "保养记录不存在", "设备保养记录不存在或已被删除，请刷新后重试");

    /** 新增保养任务或修改任务编号时，编号已被其他有效记录占用时触发。 */
    public static final ErrorCode EQUIPMENT_MAINTENANCE_RECORD_NO_DUPLICATE =
            new ErrorCode("A0506", "保养任务编号已存在", "保养任务编号重复，请更换编号后重试");

    /** 开始、完成、取消或修改保养任务时，当前状态不允许目标状态迁移时触发。 */
    public static final ErrorCode EQUIPMENT_MAINTENANCE_STATUS_OPERATION_NOT_ALLOWED =
            new ErrorCode("A0440", "保养任务状态不允许当前操作", "请按照保养任务状态流程执行操作");

    /** 将保养任务置为完成时未填写 NORMAL/ABNORMAL 结果或必要异常说明时触发。 */
    public static final ErrorCode EQUIPMENT_MAINTENANCE_RESULT_REQUIRED =
            new ErrorCode("A0420", "完成保养任务必须填写保养结果", "请选择正常或异常并填写必要说明");

    /** 保存计划负责人或任务执行人时，用户不存在、已逻辑删除或账号未启用时触发。 */
    public static final ErrorCode EQUIPMENT_MAINTENANCE_USER_NOT_EXISTS =
            new ErrorCode("A0402", "保养负责人或执行人不可用", "请选择存在、未删除且已启用的系统用户");

    /** 保存开始或完成时间时，时间晚于当前时刻或完成时间早于开始时间时触发。 */
    public static final ErrorCode EQUIPMENT_MAINTENANCE_TIME_INVALID =
            new ErrorCode("A0420", "保养任务时间不合法", "开始和完成时间不能晚于当前时间，完成时间不能早于开始时间");

    /** 尝试修改或删除 COMPLETED、CANCELLED 终态保养记录，破坏历史审计凭据时触发。 */
    public static final ErrorCode EQUIPMENT_MAINTENANCE_TERMINAL_RECORD_IMMUTABLE =
            new ErrorCode("A0440", "终态保养任务不可修改或删除", "已完成或已取消的保养任务必须作为历史记录保留");

    /** 删除设备台账前发现仍有关联保养计划或有效保养历史记录时触发。 */
    public static final ErrorCode EQUIPMENT_LEDGER_HAS_MAINTENANCE =
            new ErrorCode("A0440", "该设备存在保养业务，不允许删除", "历史保养计划和记录必须与原设备保持关联");

    /** 错误码常量容器不允许实例化。 */
    private EquipmentErrorCodeConstants() {
    }
}
