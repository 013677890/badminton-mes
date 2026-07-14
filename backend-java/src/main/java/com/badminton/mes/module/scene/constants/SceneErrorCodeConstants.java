package com.badminton.mes.module.scene.constants;

import com.badminton.mes.common.core.ErrorCode;

/** M2 现场执行模块错误码。 @author 刘涵 */
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
    public static final ErrorCode REPORT_NOT_EXISTS = error("A0402", "报工记录不存在");
    public static final ErrorCode REPORT_STATUS_INVALID = error("A0440", "当前任务或工序不允许报工");
    public static final ErrorCode REPORT_QUANTITY_INVALID = error("A0420", "报工数量关系不合法");
    public static final ErrorCode REPORT_BARCODE_REQUIRED = error("A0440", "当前生产参数要求报工前扫描合法条码");
    public static final ErrorCode REPORT_ALREADY_REVERSED = error("A0506", "该报工已经完成冲销");
    public static final ErrorCode COMPLETION_NOT_EXISTS = error("A0402", "完工单不存在");
    public static final ErrorCode COMPLETION_STATUS_INVALID = error("A0440", "完工单当前状态不允许执行该操作");
    public static final ErrorCode COMPLETION_QUANTITY_INVALID = error("A0420", "完工数量超过可完工数量");
    public static final ErrorCode COMPLETION_SYNC_RETRY_EXCEEDED = error("A0440", "完工同步重试次数已达到上限");
    public static final ErrorCode COMPLETION_SYNC_FAILED = error("C0001", "完工单同步外部系统失败");
    public static final ErrorCode REPAIR_NOT_EXISTS = error("A0402", "返修工单不存在或不在授权范围内");
    public static final ErrorCode REPAIR_STATUS_INVALID = error("A0440", "返修工单当前状态不允许执行该操作");
    public static final ErrorCode REPAIR_QUANTITY_INVALID = error("A0420", "返修数量超过来源不良可返修数量");
    public static final ErrorCode REPAIR_RECHECK_INVALID = error("A0440", "返修复检结果或数量不合法");

    private static ErrorCode error(String code, String message) {
        return new ErrorCode(code, message, message + "，请检查数据后重试");
    }

    private SceneErrorCodeConstants() {
    }
}
