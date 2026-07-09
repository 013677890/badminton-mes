package com.badminton.mes.module.production.constants;

import com.badminton.mes.common.core.ErrorCode;

/**
 * 生产订单模块错误码，与业务代码就近维护。
 *
 * <p>编码复用《Java开发手册(黄山版)》附表 3 宏观错误码(ERRCODE-006)，
 * 业务细节由 message 承载(ERRCODE-008)：
 * A0402 无效的用户输入 / A0420 参数值超出范围 / A0440 用户操作异常 / A0506 用户重复请求。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
public final class ProductionErrorCodeConstants {

    /** 生产工单不存在或已删除 */
    public static final ErrorCode WORK_ORDER_NOT_EXISTS =
            new ErrorCode("A0402", "生产工单不存在", "工单不存在或已被删除，请刷新后重试");

    /** 工单号重复(应用层先查校验，数据库唯一索引 uk_work_order_no 兜底) */
    public static final ErrorCode WORK_ORDER_NO_DUPLICATE =
            new ErrorCode("A0506", "工单号已存在", "工单号重复，请更换工单号后重试");

    /** 计划完成时间早于计划开始时间 */
    public static final ErrorCode WORK_ORDER_PLAN_TIME_INVALID =
            new ErrorCode("A0420", "计划完成时间不能早于计划开始时间", "请检查计划开始与完成时间");

    /** 非"已创建"状态的工单不允许修改 */
    public static final ErrorCode WORK_ORDER_STATUS_NOT_ALLOW_UPDATE =
            new ErrorCode("A0440", "当前工单状态不允许修改", "工单已下达或已进入生产，不能直接修改");

    /** 非"已创建"状态的工单不允许删除(已审核/已下达单据不允许随意删除) */
    public static final ErrorCode WORK_ORDER_STATUS_NOT_ALLOW_DELETE =
            new ErrorCode("A0440", "当前工单状态不允许删除", "工单已下达或已进入生产，不能删除");

    /** 非"已创建"状态的工单不允许下达 */
    public static final ErrorCode WORK_ORDER_STATUS_NOT_ALLOW_RELEASE =
            new ErrorCode("A0440", "当前工单状态不允许下达", "只有处于已创建状态的工单才能下达");

    /** 未维护 BOM 或工艺路线的工单不允许下达(业务规则：见 01-生产订单需求分析) */
    public static final ErrorCode WORK_ORDER_RELEASE_MISSING_BOM_ROUTING =
            new ErrorCode("A0440", "工单未维护 BOM 或工艺路线，不允许下达", "请先维护 BOM 与工艺路线再下达");

    /** 产品不存在或已停用 */
    public static final ErrorCode PRODUCT_NOT_EXISTS =
            new ErrorCode("A0402", "产品不存在或已停用", "所选产品不可用，请重新选择");

    /** 车间不存在或已停用 */
    public static final ErrorCode WORKSHOP_NOT_EXISTS =
            new ErrorCode("A0402", "车间不存在或已停用", "所选车间不可用，请重新选择");

    /** 非"已下达/生产中"状态的工单不允许暂停 */
    public static final ErrorCode WORK_ORDER_STATUS_NOT_ALLOW_PAUSE =
            new ErrorCode("A0440", "当前工单状态不允许暂停", "只有已下达或生产中的工单才能暂停");

    /** 非"暂停"状态的工单不允许恢复 */
    public static final ErrorCode WORK_ORDER_STATUS_NOT_ALLOW_RESUME =
            new ErrorCode("A0440", "当前工单状态不允许恢复", "只有暂停中的工单才能恢复");

    /** 非"已下达/生产中"状态的工单不允许完工 */
    public static final ErrorCode WORK_ORDER_STATUS_NOT_ALLOW_FINISH =
            new ErrorCode("A0440", "当前工单状态不允许完工", "只有已下达或生产中的工单才能完工");

    /** 非"已完工"状态的工单不允许关闭 */
    public static final ErrorCode WORK_ORDER_STATUS_NOT_ALLOW_CLOSE =
            new ErrorCode("A0440", "当前工单状态不允许关闭", "只有已完工的工单才能关闭");

    /** 非"已创建/已下达"状态的工单不允许作废 */
    public static final ErrorCode WORK_ORDER_STATUS_NOT_ALLOW_CANCEL =
            new ErrorCode("A0440", "当前工单状态不允许作废", "只有已创建或已下达的工单才能作废");

    /** BOM 不存在、已删除或未生效 */
    public static final ErrorCode BOM_NOT_AVAILABLE =
            new ErrorCode("A0402", "BOM 不存在或未生效", "所选 BOM 不可用，请重新维护");

    /** BOM 未维护明细，无法生成物料需求 */
    public static final ErrorCode BOM_DETAIL_EMPTY =
            new ErrorCode("A0440", "BOM 未维护明细", "请先维护 BOM 物料明细再下达工单");

    /** 已下达工单修改计划必须填写变更原因(业务规则：见 01-生产订单需求分析) */
    public static final ErrorCode WORK_ORDER_CHANGE_REASON_REQUIRED =
            new ErrorCode("A0402", "修改已下达工单必须填写变更原因", "请填写变更原因后重试");

    /** 计划数量不能低于已派工数量 */
    public static final ErrorCode WORK_ORDER_PLAN_LESS_THAN_DISPATCHED =
            new ErrorCode("A0420", "计划数量不能低于已派工数量", "请检查计划数量与已派工数量");

    /** 完工数量超过计划数量与允许超产比例的上限 */
    public static final ErrorCode WORK_ORDER_FINISH_EXCEED_LIMIT =
            new ErrorCode("A0420", "完工数量超过允许超产上限", "完工数量超出计划数量与超产比例限制，请核对报工数据");

    /** 计划变更后物料需求数量低于已领数量 */
    public static final ErrorCode WORK_ORDER_MATERIAL_ISSUED_EXCEED =
            new ErrorCode("A0420", "物料需求数量不能低于已领数量", "计划数量下调后物料需求低于已领数量，请先处理领料再变更计划");

    private ProductionErrorCodeConstants() {
    }
}
