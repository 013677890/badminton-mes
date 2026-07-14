package com.badminton.mes.module.scene.constants;

import com.badminton.mes.common.core.ErrorCode;

/** 现场执行模块错误码。 */
public final class SceneErrorCodeConstants {

    public static final ErrorCode PARAM_NOT_EXISTS = error("A0402", "生产参数不存在");
    public static final ErrorCode PARAM_DUPLICATE = error("A0506", "同一作用域已存在该生产参数");
    public static final ErrorCode PARAM_VALUE_INVALID = error("A0402", "生产参数值与类型不匹配");
    public static final ErrorCode TASK_NOT_EXISTS = error("A0402", "生产任务不存在或不在授权范围内");
    public static final ErrorCode TASK_STATUS_INVALID = error("A0440", "生产任务当前状态不允许执行该操作");
    public static final ErrorCode TASK_QUANTITY_EXCEEDED = error("A0420", "任务数量超过工单未派数量");
    public static final ErrorCode WORK_ORDER_NOT_AVAILABLE = error("A0402", "来源工单不存在或未下达");
    public static final ErrorCode LINE_NOT_AVAILABLE = error("A0402", "产线不存在、已停用或不属于工单车间");
    public static final ErrorCode ROUTING_NOT_AVAILABLE = error("A0402", "工艺路线、工序或 SOP 不可用");
    public static final ErrorCode DISPATCH_ALREADY_EXISTS = error("A0506", "任务已存在有效工序派工");
    public static final ErrorCode DISPATCH_NOT_EXISTS = error("A0402", "工序派工不存在");
    public static final ErrorCode DISPATCH_STATUS_INVALID = error("A0440", "工序派工当前状态不允许执行该操作");
    public static final ErrorCode OPERATION_NOT_EXISTS = error("A0402", "工序任务不存在或不在授权范围内");
    public static final ErrorCode OPERATION_STATUS_INVALID = error("A0440", "工序当前状态不允许执行该操作");
    public static final ErrorCode OPERATION_SEQUENCE_INVALID = error("A0440", "关键或必检工序必须按工艺顺序执行");
    public static final ErrorCode OPERATION_SCAN_REQUIRED = error("A0440", "该工序必须先扫码确认批次");
    public static final ErrorCode BARCODE_NOT_MATCH = error("A0402", "扫码批次与任务、产品或工序不匹配");
    public static final ErrorCode DATA_SCOPE_DENIED = error("A0301", "无权访问该车间、产线或工序任务");
    public static final ErrorCode NUMBER_GENERATE_FAILED = error("B0001", "现场单号生成失败");

    public static final ErrorCode PRODUCTION_TASK_NOT_EXISTS =
            new ErrorCode("A0402", "现场生产任务不存在", "请先下发对应派工单");
    public static final ErrorCode WORK_REPORT_NOT_EXISTS =
            new ErrorCode("A0402", "生产报工不存在", "报工不存在或已删除");
    public static final ErrorCode WORK_REPORT_STATUS_INVALID =
            new ErrorCode("A0440", "生产报工状态不允许审核", "只有待确认报工可以审核");
    public static final ErrorCode WORK_REPORT_EMPLOYEE_REQUIRED =
            new ErrorCode("A0402", "生产报工缺少员工", "请指定实际操作员工");
    public static final ErrorCode WORK_REPORT_PROCESS_INVALID =
            new ErrorCode("A0402", "生产报工工序不可用", "工序不存在、已停用或已删除");
    public static final ErrorCode PROCESS_TASK_NOT_EXISTS =
            new ErrorCode("A0402", "现场工序任务不存在", "报工工序不属于当前生产任务");
    public static final ErrorCode PRODUCTION_TASK_STATUS_INVALID =
            new ErrorCode("A0440", "现场生产任务状态不允许操作", "已取消任务不能报工或完工");
    public static final ErrorCode COMPLETION_QUANTITY_INVALID =
            new ErrorCode("A0420", "完工数量不一致", "良品数量与不良数量之和必须等于完工数量");
    public static final ErrorCode COMPLETION_ORDER_NOT_EXISTS =
            new ErrorCode("A0402", "生产完工单不存在", "完工单不存在或已删除");
    public static final ErrorCode COMPLETION_STATUS_INVALID =
            new ErrorCode("A0440", "生产完工单状态不允许操作", "只有待审核完工单可以审核或作废");
    public static final ErrorCode COMPLETION_PROCESS_NOT_FINISHED =
            new ErrorCode("A0440", "必经工序尚未完成", "全部工序及必要质检完成后才能提交完工");
    public static final ErrorCode COMPLETION_REPORT_QUANTITY_NOT_ENOUGH =
            new ErrorCode("A0420", "可完工报工数量不足", "完工数量不能超过末工序已审核报工余额");
    public static final ErrorCode COMPLETION_TASK_WORK_ORDER_MISMATCH =
            new ErrorCode("A0420", "生产任务与工单不一致", "请使用生产任务所属的生产工单");

    private static ErrorCode error(String code, String message) {
        return new ErrorCode(code, message, message + "，请检查数据后重试");
    }

    private SceneErrorCodeConstants() {
    }
}
