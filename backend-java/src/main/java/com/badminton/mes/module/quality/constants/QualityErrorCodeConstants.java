package com.badminton.mes.module.quality.constants;

import com.badminton.mes.common.core.ErrorCode;

/**
 * 质量管理模块业务错误码。
 *
 * <p>错误码按分类主数据、检验项目、方案版本和检验单四组定义；下方注释描述服务层抛出该错误的
 * 具体前置条件，便于调用方区分不存在、规则非法、状态冲突和数据库唯一约束冲突。
 */
public final class QualityErrorCodeConstants {

    /** 分类主键无法命中未删除记录，或项目引用的分类已删除、已停用时触发。 */
    public static final ErrorCode CATEGORY_NOT_EXISTS =
            new ErrorCode("A0402", "检验分类不存在", "检验分类不存在或已删除，请刷新后重试");
    /** 创建或更新分类编码重复、删除占位编码碰撞，或数据库唯一约束兜底失败时触发。 */
    public static final ErrorCode CATEGORY_CODE_DUPLICATE =
            new ErrorCode("A0506", "检验分类编码已存在", "请更换检验分类编码后重试");
    /** 删除分类时仍存在通过 {@code categoryId} 引用它的未删除检验项目。 */
    public static final ErrorCode CATEGORY_HAS_ITEMS =
            new ErrorCode("A0440", "检验分类下存在检验项目", "请先移除或调整分类下的检验项目");
    /** 项目主键无法命中未删除记录时触发。 */
    public static final ErrorCode ITEM_NOT_EXISTS =
            new ErrorCode("A0402", "检验项目不存在", "检验项目不存在或已删除，请刷新后重试");
    /** 项目业务编码重复、删除占位编码碰撞，或数据库唯一约束兜底失败时触发。 */
    public static final ErrorCode ITEM_CODE_DUPLICATE =
            new ErrorCode("A0506", "检验项目编码已存在", "请更换检验项目编码后重试");
    /** 值类型与单位、上下限或判定方式不相容，或标准值判定缺少标准值时触发。 */
    public static final ErrorCode ITEM_RULE_INVALID =
            new ErrorCode("A0420", "检验项目判定规则不合法", "请检查值类型、单位、标准值及上下限配置");
    /** 删除检验项目时任一方案版本仍通过方案明细引用该项目。 */
    public static final ErrorCode ITEM_REFERENCED_BY_PLAN =
            new ErrorCode("A0440", "检验项目已被方案引用", "已被检验方案引用的项目不能删除");
    /** 方案版本主键无法命中未删除记录，或版本链锁定后源版本已不可见时触发。 */
    public static final ErrorCode PLAN_NOT_EXISTS =
            new ErrorCode("A0402", "检验方案不存在", "检验方案不存在或已删除，请刷新后重试");
    /** 初始版本编码重复、更新试图变更版本链编码，或数据库唯一约束兜底失败时触发。 */
    public static final ErrorCode PLAN_CODE_DUPLICATE =
            new ErrorCode("A0506", "检验方案编码已存在", "请更换方案编码，或基于已有方案创建新版本");
    /** 同一方案请求重复引用项目，或保存明细时数据库唯一约束兜底失败时触发。 */
    public static final ErrorCode PLAN_ITEMS_DUPLICATE =
            new ErrorCode("A0420", "检验方案包含重复项目", "同一检验方案不能重复配置相同检验项目");
    /** 方案项引用缺失、已删除或停用项目，或最终判定规则与项目值类型不相容时触发。 */
    public static final ErrorCode PLAN_ITEM_INVALID =
            new ErrorCode("A0420", "检验方案项目配置不合法", "请检查检验项目状态、抽样数量和判定标准");
    /** 对非草稿方案执行编辑或删除时触发。 */
    public static final ErrorCode PLAN_EDIT_NOT_ALLOWED =
            new ErrorCode("A0440", "当前检验方案不可修改", "只有草稿状态的检验方案可以修改或删除");
    /** 审核对象不是草稿、草稿无明细，或派生版本时源方案无明细时触发。 */
    public static final ErrorCode PLAN_AUDIT_NOT_ALLOWED =
            new ErrorCode("A0440", "当前检验方案不可审核", "只有包含有效检验项目的草稿方案可以审核生效");
    /** 对非生效状态方案执行停用时触发。 */
    public static final ErrorCode PLAN_DISABLE_NOT_ALLOWED =
            new ErrorCode("A0440", "当前检验方案不可停用", "只有已生效的检验方案可以停用");
    /** 审核默认方案时，同产品、客户和检验类型范围已有其他默认生效版本。 */
    public static final ErrorCode PLAN_DEFAULT_CONFLICT =
            new ErrorCode("A0506", "默认检验方案冲突", "同一产品、客户和检验类型只能有一个默认生效方案");
    /** 以草稿方案作为源版本创建新版本时触发；草稿应直接编辑而非继续派生。 */
    public static final ErrorCode PLAN_VERSION_NOT_ALLOWED =
            new ErrorCode("A0440", "当前检验方案不能创建新版本", "草稿方案请直接修改，生效或停用方案才能创建新版本");
    /** 检验单主键无法命中未删除记录时触发。 */
    public static final ErrorCode RECORD_NOT_EXISTS =
            new ErrorCode("A0402", "质量检验单不存在", "质量检验单不存在或已删除，请刷新后重试");
    /** 方案未生效、尚未到生效日、类型或产品客户范围不匹配，或缺少可生成快照的明细项目时触发。 */
    public static final ErrorCode RECORD_PLAN_UNAVAILABLE =
            new ErrorCode("A0440", "检验方案不可用于正式检验", "请选择已审核生效且检验类型匹配的方案版本");
    /** 生产检验缺少工单或请求范围与工单不一致，非生产检验缺少来源单据、单号或产品时触发。 */
    public static final ErrorCode RECORD_SOURCE_INVALID =
            new ErrorCode("A0420", "检验单来源数据不合法", "请检查生产工单、来源单据、产品和批次信息");
    /** 对非草稿检验单保存项目结果或再次提交时触发。 */
    public static final ErrorCode RECORD_EDIT_NOT_ALLOWED =
            new ErrorCode("A0440", "当前检验单不可修改", "已提交的检验单及其项目结果不能修改");
    /** 结果重复或不属于当前单据、必检值缺失、判定缺少实测值，或不合格说明/处置缺失时触发。 */
    public static final ErrorCode RECORD_RESULTS_INCOMPLETE =
            new ErrorCode("A0420", "检验结果不完整", "请填写所有必检项目，并补充不合格项目说明");
    /** 项目均合格却提交非合格结论，或存在失败项目却提交合格结论时触发。 */
    public static final ErrorCode RECORD_CONCLUSION_INVALID =
            new ErrorCode("A0420", "检验结论与项目结果不一致", "存在不合格项目时不能提交合格结论");
    /** 保存检验单时生成的单号碰撞数据库唯一约束。 */
    public static final ErrorCode RECORD_NO_DUPLICATE =
            new ErrorCode("A0506", "检验单号已存在", "请重新提交，系统将重新生成检验单号");

    private QualityErrorCodeConstants() {
    }
}
