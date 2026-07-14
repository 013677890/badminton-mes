package com.badminton.mes.module.quality.constants;

import com.badminton.mes.common.core.ErrorCode;

/** 质量管理模块错误码。 */
public final class QualityErrorCodeConstants {

    public static final ErrorCode CATEGORY_NOT_EXISTS =
            new ErrorCode("A0402", "检验分类不存在", "检验分类不存在或已删除，请刷新后重试");
    public static final ErrorCode CATEGORY_CODE_DUPLICATE =
            new ErrorCode("A0506", "检验分类编码已存在", "请更换检验分类编码后重试");
    public static final ErrorCode CATEGORY_HAS_ITEMS =
            new ErrorCode("A0440", "检验分类下存在检验项目", "请先移除或调整分类下的检验项目");
    public static final ErrorCode ITEM_NOT_EXISTS =
            new ErrorCode("A0402", "检验项目不存在", "检验项目不存在或已删除，请刷新后重试");
    public static final ErrorCode ITEM_CODE_DUPLICATE =
            new ErrorCode("A0506", "检验项目编码已存在", "请更换检验项目编码后重试");
    public static final ErrorCode ITEM_RULE_INVALID =
            new ErrorCode("A0420", "检验项目判定规则不合法", "请检查值类型、单位、标准值及上下限配置");
    public static final ErrorCode ITEM_REFERENCED_BY_PLAN =
            new ErrorCode("A0440", "检验项目已被方案引用", "已被检验方案引用的项目不能删除");
    public static final ErrorCode PLAN_NOT_EXISTS =
            new ErrorCode("A0402", "检验方案不存在", "检验方案不存在或已删除，请刷新后重试");
    public static final ErrorCode PLAN_CODE_DUPLICATE =
            new ErrorCode("A0506", "检验方案编码已存在", "请更换方案编码，或基于已有方案创建新版本");
    public static final ErrorCode PLAN_ITEMS_DUPLICATE =
            new ErrorCode("A0420", "检验方案包含重复项目", "同一检验方案不能重复配置相同检验项目");
    public static final ErrorCode PLAN_ITEM_INVALID =
            new ErrorCode("A0420", "检验方案项目配置不合法", "请检查检验项目状态、抽样数量和判定标准");
    public static final ErrorCode PLAN_EDIT_NOT_ALLOWED =
            new ErrorCode("A0440", "当前检验方案不可修改", "只有草稿状态的检验方案可以修改或删除");
    public static final ErrorCode PLAN_AUDIT_NOT_ALLOWED =
            new ErrorCode("A0440", "当前检验方案不可审核", "只有包含有效检验项目的草稿方案可以审核生效");
    public static final ErrorCode PLAN_DISABLE_NOT_ALLOWED =
            new ErrorCode("A0440", "当前检验方案不可停用", "只有已生效的检验方案可以停用");
    public static final ErrorCode PLAN_DEFAULT_CONFLICT =
            new ErrorCode("A0506", "默认检验方案冲突", "同一产品、客户和检验类型只能有一个默认生效方案");
    public static final ErrorCode PLAN_VERSION_NOT_ALLOWED =
            new ErrorCode("A0440", "当前检验方案不能创建新版本", "草稿方案请直接修改，生效或停用方案才能创建新版本");
    public static final ErrorCode RECORD_NOT_EXISTS =
            new ErrorCode("A0402", "质量检验单不存在", "质量检验单不存在或已删除，请刷新后重试");
    public static final ErrorCode RECORD_PLAN_UNAVAILABLE =
            new ErrorCode("A0440", "检验方案不可用于正式检验", "请选择已审核生效且检验类型匹配的方案版本");
    public static final ErrorCode RECORD_SOURCE_INVALID =
            new ErrorCode("A0420", "检验单来源数据不合法", "请检查生产工单、来源单据、产品和批次信息");
    public static final ErrorCode RECORD_EDIT_NOT_ALLOWED =
            new ErrorCode("A0440", "当前检验单不可修改", "已提交的检验单及其项目结果不能修改");
    public static final ErrorCode RECORD_RESULTS_INCOMPLETE =
            new ErrorCode("A0420", "检验结果不完整", "请填写所有必检项目，并补充不合格项目说明");
    public static final ErrorCode RECORD_CONCLUSION_INVALID =
            new ErrorCode("A0420", "检验结论与项目结果不一致", "存在不合格项目时不能提交合格结论");
    public static final ErrorCode RECORD_NO_DUPLICATE =
            new ErrorCode("A0506", "检验单号已存在", "请重新提交，系统将重新生成检验单号");

    private QualityErrorCodeConstants() {
    }
}
