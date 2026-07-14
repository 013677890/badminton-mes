package com.badminton.mes.module.wage.constants;

import com.badminton.mes.common.core.ErrorCode;

/**
 * 计件工资模块错误码。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
public final class WageErrorCodeConstants {

    /** 计件规则不存在 */
    public static final ErrorCode RULE_NOT_EXISTS =
            new ErrorCode("A0402", "计件规则不存在", "计件规则不存在或已删除，请刷新后重试");

    /** 计件规则发生并发修改 */
    public static final ErrorCode RULE_CONCURRENT_MODIFICATION =
            new ErrorCode("A0440", "计件规则已被其他用户修改", "规则数据已变更，请刷新后重试");

    /** 规则生效日期不合法 */
    public static final ErrorCode RULE_DATE_INVALID =
            new ErrorCode("A0420", "计件规则生效日期范围不合法", "结束日期不能早于开始日期");

    /** 规则生效期间重叠 */
    public static final ErrorCode RULE_PERIOD_OVERLAP =
            new ErrorCode("A0506", "同一工序和产品的计件规则生效期间重叠", "请调整规则生效日期");

    /** 工序未启用计件 */
    public static final ErrorCode PROCESS_NOT_PIECE_RATE =
            new ErrorCode("A0440", "工序未启用计件", "请先在工序档案中启用计件");

    /** 产品不可用 */
    public static final ErrorCode PRODUCT_NOT_AVAILABLE =
            new ErrorCode("A0402", "产品不可用", "请选择已启用的产品");

    /** 报工快照引用无效 */
    public static final ErrorCode WORK_RECORD_REFERENCE_INVALID =
            new ErrorCode("A0402", "报工快照引用的员工、工单、工序或产品无效", "请检查报工数据后重试");

    /** 报工快照与工单产品不一致 */
    public static final ErrorCode WORK_RECORD_PRODUCT_MISMATCH =
            new ErrorCode("A0420", "报工产品与生产工单产品不一致", "请修正报工产品后重试");

    /** 结算批次不存在 */
    public static final ErrorCode SETTLEMENT_NOT_EXISTS =
            new ErrorCode("A0402", "工资结算批次不存在", "结算批次不存在或已删除，请刷新后重试");

    /** 结算批次发生并发修改 */
    public static final ErrorCode SETTLEMENT_CONCURRENT_MODIFICATION =
            new ErrorCode("A0440", "工资结算批次已被其他用户修改", "结算数据已变更，请刷新后重试");

    /** 结算状态不支持当前操作 */
    public static final ErrorCode SETTLEMENT_STATUS_INVALID =
            new ErrorCode("A0440", "当前结算状态不支持该操作", "请刷新结算状态后重试");

    /** 结算日期范围不合法 */
    public static final ErrorCode SETTLEMENT_PERIOD_INVALID =
            new ErrorCode("A0420", "结算日期范围不合法", "结算周期最长 31 天且结束日期不能早于开始日期");

    /** 无可结算报工 */
    public static final ErrorCode SETTLEMENT_NO_ELIGIBLE_RECORD =
            new ErrorCode("A0402", "没有可结算的已审核报工", "请检查报工日期、员工范围或既有结算");

    /** 待结算报工数量超过保护上限 */
    public static final ErrorCode SETTLEMENT_RECORD_LIMIT_EXCEEDED =
            new ErrorCode("A0420", "单批次报工数量超过 5000 条", "请缩小结算日期或员工范围");

    /** 工资汇总分组数量超过保护上限 */
    public static final ErrorCode SUMMARY_RESULT_LIMIT_EXCEEDED =
            new ErrorCode("A0420", "工资汇总结果超过 1000 条", "请缩小统计日期或指定员工、工序范围");

    /** 结算数量合计超过数据库支持范围 */
    public static final ErrorCode SETTLEMENT_QUANTITY_OUT_OF_RANGE =
            new ErrorCode("A0420", "结算数量合计超过系统支持范围", "请缩小结算日期或员工范围");

    /** 工资金额超过系统支持范围 */
    public static final ErrorCode AMOUNT_OUT_OF_RANGE =
            new ErrorCode("A0420", "工资金额超过系统支持范围", "请降低计件单价、数量或调整金额");

    /** 报工未匹配到有效计件规则 */
    public static final ErrorCode SETTLEMENT_RULE_MISSING =
            new ErrorCode("A0402", "部分报工未匹配到有效计件规则", "请补齐对应日期的计件规则后重试");

    /** 来源报工被其他结算占用 */
    public static final ErrorCode WORK_RECORD_ALREADY_SETTLED =
            new ErrorCode("A0506", "来源报工已被其他结算占用", "请刷新后重新计算结算");

    /** 结算明细不存在 */
    public static final ErrorCode SETTLEMENT_DETAIL_NOT_EXISTS =
            new ErrorCode("A0402", "工资结算明细不存在", "结算明细不存在或已失效，请刷新后重试");

    private WageErrorCodeConstants() {
    }
}
