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

    /** 工艺路线不存在、未生效或未绑定工单产品 */
    public static final ErrorCode WORK_ORDER_ROUTING_NOT_AVAILABLE =
            new ErrorCode("A0440", "工单工艺路线不可用于下达", "请选择已生效且适用于当前产品的工艺路线");

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

    /** BOM 明细引用的物料不存在、已删除或已停用 */
    public static final ErrorCode MATERIAL_NOT_AVAILABLE =
            new ErrorCode("A0402", "BOM 明细存在不可用物料", "请检查 BOM 物料档案后重试");

    /** 工艺路线不存在、已删除或未生效 */
    public static final ErrorCode ROUTING_NOT_AVAILABLE =
            new ErrorCode("A0402", "工艺路线不存在或未生效", "所选工艺路线不可用，请重新维护");

    /** 工艺路线未绑定工单产品 */
    public static final ErrorCode ROUTING_PRODUCT_NOT_MATCH =
            new ErrorCode("A0402", "工艺路线未绑定工单产品", "请为产品维护可用的工艺路线后重试");

    /** 工艺路线明细为空或工序顺序不连续 */
    public static final ErrorCode ROUTING_DETAIL_INVALID =
            new ErrorCode("A0440", "工艺路线明细不完整", "请维护从 1 开始且连续的工序顺序");

    /** 工艺路线明细引用的工序不存在、已删除或已停用 */
    public static final ErrorCode ROUTING_PROCESS_NOT_AVAILABLE =
            new ErrorCode("A0402", "工艺路线存在不可用工序", "请检查工艺路线中的工序档案后重试");

    /** 工艺路线明细引用的 SOP 不存在、未生效或生效日期未到 */
    public static final ErrorCode ROUTING_SOP_NOT_AVAILABLE =
            new ErrorCode("A0402", "工艺路线存在不可用 SOP", "请检查工艺路线中的 SOP 后重试");

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

    /** 非"已下达/生产中"状态的工单不允许齐套分析 */
    public static final ErrorCode KIT_ANALYSIS_ORDER_STATUS_INVALID =
            new ErrorCode("A0440", "当前工单状态不允许齐套分析", "只有已下达或生产中的工单才能进行齐套分析");

    /** 工单无物料需求，无法齐套分析(未下达或物料需求未生成) */
    public static final ErrorCode KIT_ANALYSIS_MATERIAL_EMPTY =
            new ErrorCode("A0440", "工单没有物料需求，无法齐套分析", "请先下达工单生成物料需求");

    /** 欠料处理记录不存在或已删除 */
    public static final ErrorCode SHORTAGE_HANDLE_NOT_EXISTS =
            new ErrorCode("A0402", "欠料处理记录不存在", "处理记录不存在或已被删除，请刷新后重试");

    /** 欠料处理记录已解决，不允许重复解决 */
    public static final ErrorCode SHORTAGE_HANDLE_ALREADY_RESOLVED =
            new ErrorCode("A0506", "欠料处理记录已解决", "该处理记录已标记解决，请勿重复操作");

    /** 派工单不存在或已删除 */
    public static final ErrorCode DISPATCH_ORDER_NOT_EXISTS =
            new ErrorCode("A0402", "派工单不存在", "派工单不存在或已被删除，请刷新后重试");

    /** 非"已下达/生产中"状态的工单不允许派工 */
    public static final ErrorCode DISPATCH_WORK_ORDER_STATUS_INVALID =
            new ErrorCode("A0440", "当前工单状态不允许派工", "只有已下达或生产中的工单才能派工");

    /** 派工数量超过工单未派数量(计划数×(1+超产比例)-已派) */
    public static final ErrorCode DISPATCH_QUANTITY_EXCEED =
            new ErrorCode("A0420", "派工数量超过工单未派数量", "派工数量超出工单剩余可派数量，请调整数量");

    /** 同产线同日同班次累计排产超出产能 */
    public static final ErrorCode DISPATCH_CAPACITY_EXCEED =
            new ErrorCode("A0420", "超出产线班次剩余产能", "该产线该班次排产已满，请更换产线、日期或班次");

    /** 产线不存在、已删除或已停用 */
    public static final ErrorCode DISPATCH_LINE_NOT_AVAILABLE =
            new ErrorCode("A0402", "产线不存在或已停用", "所选产线不可用，请重新选择");

    /** 班次不存在、已删除或已停用 */
    public static final ErrorCode DISPATCH_SHIFT_NOT_AVAILABLE =
            new ErrorCode("A0402", "班次不存在或已停用", "所选班次不可用，请重新选择");

    /** 排产日期为非工作日 */
    public static final ErrorCode DISPATCH_DATE_NOT_WORKDAY =
            new ErrorCode("A0440", "排产日期为非工作日", "所选日期为非工作日，请更换排产日期");

    /** 派工计划结束时间不晚于开始时间 */
    public static final ErrorCode DISPATCH_PLAN_TIME_INVALID =
            new ErrorCode("A0420", "计划结束时间必须晚于开始时间", "请检查派工计划开始与结束时间");

    /** 当前派工单状态不允许修改 */
    public static final ErrorCode DISPATCH_STATUS_NOT_ALLOW_UPDATE =
            new ErrorCode("A0440", "当前派工单状态不允许修改", "执行中、已完成或已取消的派工单不能修改");

    /** 当前派工单状态不允许审核 */
    public static final ErrorCode DISPATCH_STATUS_NOT_ALLOW_AUDIT =
            new ErrorCode("A0440", "当前派工单状态不允许审核", "只有待审核的派工单才能审核");

    /** 当前派工单状态不允许下发 */
    public static final ErrorCode DISPATCH_STATUS_NOT_ALLOW_ISSUE =
            new ErrorCode("A0440", "当前派工单状态不允许下发", "只有已审核的派工单才能下发");

    /** 当前派工单状态不允许取消 */
    public static final ErrorCode DISPATCH_STATUS_NOT_ALLOW_CANCEL =
            new ErrorCode("A0440", "当前派工单状态不允许取消", "执行中、已完成或已取消的派工单不能取消");

    /** 已下发派工单调整必须填写原因 */
    public static final ErrorCode DISPATCH_ADJUST_REASON_REQUIRED =
            new ErrorCode("A0402", "下发后调整必须填写原因", "请填写调整原因后重试");

    /** 车间基础资料不存在 */
    public static final ErrorCode WORKSHOP_MASTER_NOT_EXISTS =
            new ErrorCode("A0402", "车间不存在", "车间不存在或已删除，请刷新后重试");

    /** 车间编码重复 */
    public static final ErrorCode WORKSHOP_CODE_DUPLICATE =
            new ErrorCode("A0506", "车间编码已存在", "车间编码重复，请更换后重试");

    /** 车间主管用户不可用 */
    public static final ErrorCode WORKSHOP_MANAGER_NOT_AVAILABLE =
            new ErrorCode("A0402", "车间主管不存在或未启用", "请选择已启用的车间主管");

    /** 车间并发修改 */
    public static final ErrorCode WORKSHOP_CONCURRENT_MODIFICATION =
            new ErrorCode("A0440", "车间已被其他用户修改", "车间数据已变化，请刷新后重试");

    /** 车间存在当前业务引用，不能停用 */
    public static final ErrorCode WORKSHOP_ACTIVE_REFERENCE_EXISTS =
            new ErrorCode("A0440", "车间仍被当前业务引用", "请先停用产线、结束工单并调整启用用户");

    /** 车间存在历史业务引用，不能删除 */
    public static final ErrorCode WORKSHOP_REFERENCE_EXISTS =
            new ErrorCode("A0440", "车间已有业务引用", "已使用车间只能停用，不能删除");

    /** 产线基础资料不存在 */
    public static final ErrorCode PRODUCTION_LINE_NOT_EXISTS =
            new ErrorCode("A0402", "产线不存在", "产线不存在或已删除，请刷新后重试");

    /** 产线编码重复 */
    public static final ErrorCode PRODUCTION_LINE_CODE_DUPLICATE =
            new ErrorCode("A0506", "产线编码已存在", "产线编码重复，请更换后重试");

    /** 产线所属车间不可用 */
    public static final ErrorCode PRODUCTION_LINE_WORKSHOP_NOT_AVAILABLE =
            new ErrorCode("A0402", "产线所属车间不存在或未启用", "请选择已启用的车间");

    /** 产线所属车间不允许修改 */
    public static final ErrorCode PRODUCTION_LINE_WORKSHOP_IMMUTABLE =
            new ErrorCode("A0440", "产线所属车间不允许修改", "如需调整归属，请停用旧产线后新建产线");

    /** 产线并发修改 */
    public static final ErrorCode PRODUCTION_LINE_CONCURRENT_MODIFICATION =
            new ErrorCode("A0440", "产线已被其他用户修改", "产线数据已变化，请刷新后重试");

    /** 产线存在当前业务引用，不能停用 */
    public static final ErrorCode PRODUCTION_LINE_ACTIVE_REFERENCE_EXISTS =
            new ErrorCode("A0440", "产线仍被当前派工或人员引用", "请先结束派工并调整启用用户");

    /** 产线存在历史业务引用，不能删除 */
    public static final ErrorCode PRODUCTION_LINE_REFERENCE_EXISTS =
            new ErrorCode("A0440", "产线已有业务引用", "已使用产线只能停用，不能删除");

    /** 产品主档不存在 */
    public static final ErrorCode PRODUCT_MASTER_NOT_EXISTS =
            new ErrorCode("A0402", "产品不存在", "产品不存在或已删除，请刷新后重试");

    /** 产品编码重复 */
    public static final ErrorCode PRODUCT_CODE_DUPLICATE =
            new ErrorCode("A0506", "产品编码已存在", "产品编码重复，请更换后重试");

    /** 产品主档并发修改 */
    public static final ErrorCode PRODUCT_CONCURRENT_MODIFICATION =
            new ErrorCode("A0440", "产品已被其他用户修改", "产品数据已变化，请刷新后重试");

    /** 产品存在当前业务引用，不能停用 */
    public static final ErrorCode PRODUCT_ACTIVE_REFERENCE_EXISTS =
            new ErrorCode("A0440", "产品仍被当前生产业务引用", "请先结束工单并停用 BOM、路线和计件规则");

    /** 产品存在历史业务引用，不能删除 */
    public static final ErrorCode PRODUCT_REFERENCE_EXISTS =
            new ErrorCode("A0440", "产品已有业务引用", "已使用产品只能停用，不能删除");

    /** 计量单位不可用 */
    public static final ErrorCode UNIT_NOT_AVAILABLE =
            new ErrorCode("A0402", "计量单位不存在或未启用", "请选择已启用的计量单位");

    /** 物料主档不存在 */
    public static final ErrorCode MATERIAL_NOT_EXISTS =
            new ErrorCode("A0402", "物料不存在", "物料不存在或已删除，请刷新后重试");

    /** 物料编码重复 */
    public static final ErrorCode MATERIAL_CODE_DUPLICATE =
            new ErrorCode("A0506", "物料编码已存在", "物料编码重复，请更换后重试");

    /** 物料主档并发修改 */
    public static final ErrorCode MATERIAL_CONCURRENT_MODIFICATION =
            new ErrorCode("A0440", "物料已被其他用户修改", "物料数据已变化，请刷新后重试");

    /** 物料存在当前业务引用，不能停用 */
    public static final ErrorCode MATERIAL_ACTIVE_REFERENCE_EXISTS =
            new ErrorCode("A0440", "物料仍被生效 BOM 或未结束工单引用", "请先处理相关 BOM 和生产工单");

    /** 物料存在历史业务引用，不能删除 */
    public static final ErrorCode MATERIAL_REFERENCE_EXISTS =
            new ErrorCode("A0440", "物料已有业务引用", "已使用物料只能停用，不能删除");

    /** 被使用物料不允许改变计量单位 */
    public static final ErrorCode MATERIAL_UNIT_IMMUTABLE =
            new ErrorCode("A0440", "已使用物料不能修改计量单位", "请保留原计量单位或新建物料编码");

    /** BOM 主档不存在 */
    public static final ErrorCode BOM_NOT_EXISTS =
            new ErrorCode("A0402", "BOM 不存在", "BOM 不存在或已删除，请刷新后重试");

    /** BOM 编码重复 */
    public static final ErrorCode BOM_CODE_DUPLICATE =
            new ErrorCode("A0506", "BOM 编码已存在", "BOM 编码重复，请更换后重试");

    /** 产品 BOM 版本重复 */
    public static final ErrorCode BOM_PRODUCT_VERSION_DUPLICATE =
            new ErrorCode("A0506", "产品 BOM 版本已存在", "请为该产品填写新的 BOM 版本");

    /** BOM 并发修改 */
    public static final ErrorCode BOM_CONCURRENT_MODIFICATION =
            new ErrorCode("A0440", "BOM 已被其他用户修改", "BOM 数据已变化，请刷新后重试");

    /** 只有 BOM 草稿允许修改删除 */
    public static final ErrorCode BOM_NOT_DRAFT =
            new ErrorCode("A0440", "只有草稿 BOM 允许修改或删除", "请创建新版本后再修改");

    /** BOM 状态不支持当前操作 */
    public static final ErrorCode BOM_STATUS_INVALID =
            new ErrorCode("A0440", "当前 BOM 状态不支持该操作", "请刷新 BOM 状态后重试");

    /** BOM 明细业务校验失败 */
    public static final ErrorCode BOM_DETAIL_INVALID =
            new ErrorCode("A0420", "BOM 明细不完整或包含重复物料", "请检查物料、标准用量和损耗率");

    /** BOM 明细引用的物料不可用 */
    public static final ErrorCode BOM_MATERIAL_NOT_AVAILABLE =
            new ErrorCode("A0402", "BOM 明细物料不存在或未启用", "请选择已启用的物料");

    /** BOM 已被工单引用 */
    public static final ErrorCode BOM_REFERENCED_BY_WORK_ORDER =
            new ErrorCode("A0440", "BOM 已被生产工单引用", "已使用 BOM 不能删除");

    private ProductionErrorCodeConstants() {
    }
}
