package com.badminton.mes.module.barcode.constants;

import com.badminton.mes.common.core.ErrorCode;

/**
 * 条码应用模块错误码，与业务代码就近维护。
 *
 * <p>编码复用《Java开发手册(黄山版)》附表 3 宏观错误码(ERRCODE-006)，
 * 业务细节由 message 承载(ERRCODE-008)：
 * A0402 无效的用户输入 / A0420 参数值超出范围 / A0440 用户操作异常 / A0506 用户重复请求。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public final class BarcodeErrorCodeConstants {

    /** 条码类型不存在或已删除 */
    public static final ErrorCode BARCODE_TYPE_NOT_EXISTS =
            new ErrorCode("A0402", "条码类型不存在", "条码类型不存在或已被删除，请刷新后重试");

    /** 条码类型编码重复(应用层先查校验，数据库唯一索引 uk_type_code 兜底) */
    public static final ErrorCode BARCODE_TYPE_CODE_DUPLICATE =
            new ErrorCode("A0506", "条码类型编码已存在", "类型编码重复，请更换编码后重试");

    /** 条码类型已处于启用状态，无需重复启用 */
    public static final ErrorCode BARCODE_TYPE_ALREADY_ENABLED =
            new ErrorCode("A0440", "条码类型已是启用状态", "该条码类型已启用，无需重复操作");

    /** 条码类型已处于停用状态，无需重复停用 */
    public static final ErrorCode BARCODE_TYPE_ALREADY_DISABLED =
            new ErrorCode("A0440", "条码类型已是停用状态", "该条码类型已停用，无需重复操作");

    /** 已被条码规则或应用规则使用的类型不允许删除(02-条码应用需求分析) */
    public static final ErrorCode BARCODE_TYPE_IN_USE_NOT_DELETE =
            new ErrorCode("A0440", "条码类型已被条码规则或应用规则使用，不允许删除",
                    "该类型已被规则引用，请先处理关联规则");

    /** 条码类型不存在或未启用，规则/应用规则引用前校验 */
    public static final ErrorCode BARCODE_TYPE_NOT_AVAILABLE =
            new ErrorCode("A0402", "条码类型不存在或已停用", "所选条码类型不可用，请重新选择");

    /** 条码规则不存在或已删除 */
    public static final ErrorCode BARCODE_RULE_NOT_EXISTS =
            new ErrorCode("A0402", "条码规则不存在", "条码规则不存在或已被删除，请刷新后重试");

    /** 条码规则编码重复(应用层先查校验，数据库唯一索引 uk_rule_code 兜底) */
    public static final ErrorCode BARCODE_RULE_CODE_DUPLICATE =
            new ErrorCode("A0506", "条码规则编码已存在", "规则编码重复，请更换编码后重试");

    /** 条码规则已处于启用状态 */
    public static final ErrorCode BARCODE_RULE_ALREADY_ENABLED =
            new ErrorCode("A0440", "条码规则已是启用状态", "该规则已启用，无需重复操作");

    /** 条码规则已处于停用状态 */
    public static final ErrorCode BARCODE_RULE_ALREADY_DISABLED =
            new ErrorCode("A0440", "条码规则已是停用状态", "该规则已停用，无需重复操作");

    /** 已被应用规则引用或已产生流水的规则不允许删除(接口文档：删除未使用规则) */
    public static final ErrorCode BARCODE_RULE_IN_USE_NOT_DELETE =
            new ErrorCode("A0440", "条码规则已被应用规则引用或已生成条码，不允许删除",
                    "该规则已被使用，请先处理关联应用规则");

    /** 规则组成配置不合法，message 携带逐条错误说明 */
    public static final ErrorCode BARCODE_RULE_CONFIG_INVALID =
            new ErrorCode("A0402", "条码规则配置不合法", "规则组成配置有误，请按提示修正后重试");

    /** 组合条码时规则变量缺少业务取值 */
    public static final ErrorCode BARCODE_RULE_VARIABLE_MISSING =
            new ErrorCode("A0402", "条码规则变量缺少取值", "生成条码缺少变量取值，请检查产品/产线信息");

    /** 流水号达到 10^位数-1 上限，不回绕(02-条码应用需求分析：提示规则容量不足) */
    public static final ErrorCode BARCODE_RULE_SERIAL_CAPACITY_EXCEEDED =
            new ErrorCode("A0420", "流水号超出规则容量", "规则容量不足，请调整流水位数或重置周期");

    /** 生成条码长度超过 barcode_value varchar(64) 上限 */
    public static final ErrorCode BARCODE_VALUE_TOO_LONG =
            new ErrorCode("A0420", "生成条码长度超过上限 64", "条码超长，请精简规则组成");

    /** 条码模板不存在或已删除 */
    public static final ErrorCode BARCODE_TEMPLATE_NOT_EXISTS =
            new ErrorCode("A0402", "条码模板不存在", "条码模板不存在或已被删除，请刷新后重试");

    /** 条码模板编码重复(应用层先查校验，数据库唯一索引 uk_code_version 兜底) */
    public static final ErrorCode BARCODE_TEMPLATE_CODE_DUPLICATE =
            new ErrorCode("A0506", "条码模板编码已存在", "模板编码重复，请更换编码后重试");

    /** 条码模板已处于启用状态 */
    public static final ErrorCode BARCODE_TEMPLATE_ALREADY_ENABLED =
            new ErrorCode("A0440", "条码模板已是启用状态", "该模板已启用，无需重复操作");

    /** 条码模板已处于停用状态 */
    public static final ErrorCode BARCODE_TEMPLATE_ALREADY_DISABLED =
            new ErrorCode("A0440", "条码模板已是停用状态", "该模板已停用，无需重复操作");

    /** 模板必须包含条码值或二维码值字段(02-条码应用需求分析) */
    public static final ErrorCode BARCODE_TEMPLATE_MISSING_BARCODE_FIELD =
            new ErrorCode("A0402", "模板必须包含条码或二维码字段", "请为模板添加条码或二维码字段");

    /** 条码规则不存在或未启用，应用规则引用前校验 */
    public static final ErrorCode BARCODE_RULE_NOT_AVAILABLE =
            new ErrorCode("A0402", "条码规则不存在或已停用", "所选条码规则不可用，请重新选择");

    /** 条码模板不存在或未启用，应用规则引用前校验 */
    public static final ErrorCode BARCODE_TEMPLATE_NOT_AVAILABLE =
            new ErrorCode("A0402", "条码模板不存在或已停用", "所选标签模板不可用，请重新选择");

    /** 产品不存在或已停用(B 组只读校验 A 组 base_product) */
    public static final ErrorCode PRODUCT_NOT_AVAILABLE =
            new ErrorCode("A0402", "产品不存在或已停用", "所选产品不可用，请重新选择");

    /** 物料不存在或已停用(B 组只读校验 A 组 base_material) */
    public static final ErrorCode MATERIAL_NOT_AVAILABLE =
            new ErrorCode("A0402", "物料不存在或已停用", "所选物料不可用，请重新选择");

    /** 条码应用规则不存在或已删除 */
    public static final ErrorCode BARCODE_APPLY_RULE_NOT_EXISTS =
            new ErrorCode("A0402", "条码应用规则不存在", "应用规则不存在或已被删除，请刷新后重试");

    /** 对象类型与产品/物料取值不匹配 */
    public static final ErrorCode BARCODE_APPLY_RULE_OBJECT_MISMATCH =
            new ErrorCode("A0402", "对象类型与产品/物料取值不匹配",
                    "产品类对象必须且只能选择产品，物料类对象必须且只能选择物料");

    /** 来源为规则生成时条码规则必填 */
    public static final ErrorCode BARCODE_APPLY_RULE_RULE_REQUIRED =
            new ErrorCode("A0402", "来源为规则生成时必须选择条码规则", "请为规则生成来源选择条码规则");

    /** 条码规则适用类型与应用规则条码类型不一致 */
    public static final ErrorCode BARCODE_APPLY_RULE_TYPE_MISMATCH =
            new ErrorCode("A0402", "条码规则不适用于所选条码类型", "请选择与条码类型匹配的条码规则");

    /** 同对象同类型仅一条启用默认规则(应用层预检，数据库 uk_active_default 兜底) */
    public static final ErrorCode BARCODE_APPLY_RULE_DEFAULT_DUPLICATE =
            new ErrorCode("A0506", "同对象同类型已存在启用的默认规则",
                    "该产品/物料在此条码类型下已有启用默认规则，请先停用或取消默认");

    /** 条码应用规则已处于启用状态 */
    public static final ErrorCode BARCODE_APPLY_RULE_ALREADY_ENABLED =
            new ErrorCode("A0440", "应用规则已是启用状态", "该应用规则已启用，无需重复操作");

    /** 条码应用规则已处于停用状态 */
    public static final ErrorCode BARCODE_APPLY_RULE_ALREADY_DISABLED =
            new ErrorCode("A0440", "应用规则已是停用状态", "该应用规则已停用，无需重复操作");

    /** 已生成条码的应用规则不允许删除(接口文档：删除未使用应用规则) */
    public static final ErrorCode BARCODE_APPLY_RULE_IN_USE_NOT_DELETE =
            new ErrorCode("A0440", "应用规则已生成条码，不允许删除", "该应用规则已被使用，可改为停用");

    /** 应用规则不存在或未启用，条码生成前校验 */
    public static final ErrorCode BARCODE_APPLY_RULE_NOT_AVAILABLE =
            new ErrorCode("A0402", "应用规则不存在或已停用", "所选应用规则不可用，请重新选择");

    /** 传入值生成来源缺少条码值 */
    public static final ErrorCode BARCODE_INPUT_VALUE_REQUIRED =
            new ErrorCode("A0402", "传入值生成来源必须提供条码值", "请提供要登记的条码值");

    /** 外部导入来源的应用规则不能调用在线生成接口 */
    public static final ErrorCode BARCODE_APPLY_RULE_SOURCE_NOT_GENERATE =
            new ErrorCode("A0440", "该应用规则来源为外部导入，不能在线生成", "请改用条码导入接口");

    /** 传入值生成来源不支持批量生成(同一条码值无法批量落库) */
    public static final ErrorCode BARCODE_BATCH_NOT_SUPPORT_INPUT_VALUE =
            new ErrorCode("A0440", "传入值生成来源不支持批量生成", "传入值生成请使用单条生成接口");

    /** 条码不存在或已删除 */
    public static final ErrorCode BARCODE_NOT_EXISTS =
            new ErrorCode("A0402", "条码不存在", "条码不存在或已被删除，请确认条码值");

    /** 条码值已存在(应用层先查校验，数据库唯一索引 uk_barcode_value 兜底) */
    public static final ErrorCode BARCODE_VALUE_DUPLICATE =
            new ErrorCode("A0506", "条码值已存在", "条码值重复，请更换条码值后重试");

    /** 规则生成撞码重试耗尽(高并发下唯一索引冲突未能在重试内消解) */
    public static final ErrorCode BARCODE_GENERATE_CONFLICT =
            new ErrorCode("A0506", "条码生成冲突", "条码生成繁忙，请稍后重试");

    /** 已使用条码不能作废(已冻结决策) */
    public static final ErrorCode BARCODE_USED_NOT_CANCEL =
            new ErrorCode("A0440", "已使用条码不能作废", "该条码已被扫码使用，不能作废");

    /** 条码已处于作废状态 */
    public static final ErrorCode BARCODE_ALREADY_CANCELLED =
            new ErrorCode("A0440", "条码已作废", "该条码已作废，无需重复操作");

    /** 流水作用域超过 barcode_serial.serial_scope varchar(64) 上限 */
    public static final ErrorCode BARCODE_SERIAL_SCOPE_TOO_LONG =
            new ErrorCode("A0420", "流水作用域超过上限 64", "对象编码过长，请精简产品/物料编码");

    /** 生产工单不存在、已删除或不在授权车间范围内(不泄露越权工单是否存在) */
    public static final ErrorCode BARCODE_WORK_ORDER_NOT_AVAILABLE =
            new ErrorCode("A0402", "生产工单不存在或不在授权范围内", "请确认工单信息或联系管理员授权");

    /** 关联工单的产品与应用规则对象不一致 */
    public static final ErrorCode BARCODE_WORK_ORDER_PRODUCT_MISMATCH =
            new ErrorCode("A0402", "工单产品与应用规则对象不一致", "请选择与工单产品匹配的应用规则");

    /** 已作废条码不能打印 */
    public static final ErrorCode BARCODE_CANCELLED_NOT_PRINT =
            new ErrorCode("A0440", "已作废条码不能打印", "该条码已作废，不能打印标签");

    /** 重复打印必须填写原因(02-条码应用需求分析) */
    public static final ErrorCode BARCODE_REPRINT_REASON_REQUIRED =
            new ErrorCode("A0402", "重复打印必须填写原因", "请填写重复打印原因后重试");

    /** 并发重打撞打印序号唯一索引(barcode_id, print_count) */
    public static final ErrorCode BARCODE_PRINT_CONFLICT =
            new ErrorCode("A0506", "打印记录冲突", "打印请求冲突，请稍后重试");

    /** 打印快照序列化失败 */
    public static final ErrorCode BARCODE_PRINT_SNAPSHOT_ERROR =
            new ErrorCode("B0001", "打印预览快照序列化失败", "系统繁忙，请稍后重试");

    /** 非外部导入来源的应用规则不能调用导入接口 */
    public static final ErrorCode BARCODE_APPLY_RULE_SOURCE_NOT_IMPORT =
            new ErrorCode("A0440", "该应用规则来源不是外部导入", "请选择外部导入来源的应用规则");

    private BarcodeErrorCodeConstants() {
    }
}
