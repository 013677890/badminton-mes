package com.badminton.mes.module.integration.constants;

import com.badminton.mes.common.core.ErrorCode;

/**
 * 外部接口模块错误码。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
public final class IntegrationErrorCodeConstants {

    /** 计量单位不存在 */
    public static final ErrorCode UNIT_NOT_EXISTS =
            new ErrorCode("A0402", "计量单位不存在", "计量单位不存在，请检查单位编码");

    /** 已被业务引用的单位禁止修改精度 */
    public static final ErrorCode UNIT_PRECISION_IN_USE =
            new ErrorCode("A0440", "计量单位已被产品引用，不能修改精度", "该单位已被使用，请保持原精度");

    /** 计量单位编码并发重复 */
    public static final ErrorCode UNIT_CODE_DUPLICATE =
            new ErrorCode("A0506", "计量单位编码已存在", "单位编码重复，请查询最新写入结果");

    /** 外部工单已写入 */
    public static final ErrorCode EXTERNAL_WORK_ORDER_DUPLICATE =
            new ErrorCode("A0506", "外部工单已写入", "该外部工单已处理，请勿重复提交");

    /** 外部工单计划时间不合法 */
    public static final ErrorCode EXTERNAL_WORK_ORDER_PLAN_TIME_INVALID =
            new ErrorCode("A0420", "计划完成时间不能早于计划开始时间", "请检查计划开始与完成时间");

    /** 外部工单产品不可用 */
    public static final ErrorCode EXTERNAL_PRODUCT_NOT_AVAILABLE =
            new ErrorCode("A0402", "产品不存在或未启用", "请检查产品编码");

    /** 外部工单车间不可用 */
    public static final ErrorCode EXTERNAL_WORKSHOP_NOT_AVAILABLE =
            new ErrorCode("A0402", "车间不存在或未启用", "请检查车间编码");

    /** 外部工单 BOM 不可用 */
    public static final ErrorCode EXTERNAL_BOM_NOT_AVAILABLE =
            new ErrorCode("A0402", "BOM 不存在、未生效或不属于当前产品", "请检查 BOM 与产品");

    /** 外部工单路线不可用 */
    public static final ErrorCode EXTERNAL_ROUTE_NOT_AVAILABLE =
            new ErrorCode("A0402", "工艺路线未生效或未绑定当前产品", "请检查工艺路线与产品");

    /** 外部任务单关联的工单不存在或不可派工 */
    public static final ErrorCode EXTERNAL_DISPATCH_WORK_ORDER_NOT_AVAILABLE =
            new ErrorCode("A0402", "工单不存在或当前状态不允许派工", "请检查工单号与工单状态");

    /** 外部任务单产线不存在或未启用 */
    public static final ErrorCode EXTERNAL_DISPATCH_LINE_NOT_AVAILABLE =
            new ErrorCode("A0402", "产线不存在或未启用", "请检查产线编码");

    /** 外部任务单班次不存在或未启用 */
    public static final ErrorCode EXTERNAL_DISPATCH_SHIFT_NOT_AVAILABLE =
            new ErrorCode("A0402", "班次不存在或未启用", "请检查班次编码");

    /** 外部任务单计划时间不合法 */
    public static final ErrorCode EXTERNAL_DISPATCH_PLAN_TIME_INVALID =
            new ErrorCode("A0420", "计划完成时间不能早于计划开始时间", "请检查计划开始与完成时间");

    /** 接口写入发生无法归类的数据库冲突 */
    public static final ErrorCode WRITE_CONFLICT =
            new ErrorCode("B0001", "接口写入发生数据库冲突", "写入失败，请稍后重试或联系管理员");

    /** 设备计数幂等键已处理 */
    public static final ErrorCode DEVICE_COUNT_DUPLICATE =
            new ErrorCode("A0506", "设备计数请求已处理", "该设备计数已处理，请勿重复提交");

    /** 设备计数关联派工单不存在 */
    public static final ErrorCode DEVICE_COUNT_DISPATCH_NOT_FOUND =
            new ErrorCode("A0402", "派工单不存在", "请检查设备计数关联的派工单号");

    /** 设备计数关联派工单状态不可用 */
    public static final ErrorCode DEVICE_COUNT_DISPATCH_STATUS_INVALID =
            new ErrorCode("A0440", "派工单未下发或不在执行中", "只有已下发或执行中的派工单可接收设备计数");

    /** 设备计数关联工序不存在 */
    public static final ErrorCode DEVICE_COUNT_PROCESS_NOT_FOUND =
            new ErrorCode("A0402", "工序编码不存在", "请检查设备计数关联的工序编码");

    /** 设备累计计数不是正数 */
    public static final ErrorCode DEVICE_COUNT_NON_POSITIVE =
            new ErrorCode("A0420", "设备计数值必须大于零", "请检查设备累计计数值");

    /** 设备累计计数发生倒退 */
    public static final ErrorCode DEVICE_COUNT_ROLLBACK =
            new ErrorCode("A0420", "设备累计计数发生倒退", "当前计数不能小于最近一次累计计数");

    /** 设备异常池查询时间范围不合法 */
    public static final ErrorCode DEVICE_EXCEPTION_TIME_RANGE_INVALID =
            new ErrorCode("A0420", "异常查询结束时间早于开始时间", "请检查异常查询时间范围");

    /** 完工单或读取日志查询时间范围不合法 */
    public static final ErrorCode COMPLETION_TIME_RANGE_INVALID =
            new ErrorCode("A0420", "完工查询结束时间早于开始时间", "请检查完工单查询时间范围");

    /** ERP 同步：产品编码不存在 */
    public static final ErrorCode ERP_PRODUCT_NOT_AVAILABLE =
            new ErrorCode("A0402", "产品编码不存在", "ERP 产品编码在 MES 中不存在或未启用");

    /** ERP 同步：计划数量必须大于 0 */
    public static final ErrorCode ERP_TASK_QUANTITY_INVALID =
            new ErrorCode("A0420", "计划数量必须大于 0", "ERP 任务单数量不合法");

    /** ERP 同步：车间不存在或未启用 */
    public static final ErrorCode ERP_TASK_WORKSHOP_NOT_AVAILABLE =
            new ErrorCode("A0402", "车间不存在或未启用", "ERP 任务单车间在 MES 中不存在或未启用");

    /** ERP 同步：计划完成时间不能早于计划开始时间 */
    public static final ErrorCode ERP_TASK_PLAN_TIME_INVALID =
            new ErrorCode("A0420", "计划完成时间不能早于计划开始时间", "请检查 ERP 任务单计划时间");

    /** ERP 同步：来源数据关键字段不完整 */
    public static final ErrorCode ERP_SOURCE_DATA_INVALID =
            new ErrorCode("A0420", "ERP 来源数据不完整", "请检查来源单号、编码、名称和时间等关键字段");

    /** ERP 工艺同步并发重复 */
    public static final ErrorCode ERP_CRAFT_DUPLICATE =
            new ErrorCode("A0506", "ERP 工艺版本已同步", "该 ERP 工艺版本已处理，请勿重复同步");

    /** ERP 同步：工序编码不存在 */
    public static final ErrorCode ERP_CRAFT_PROCESS_NOT_AVAILABLE =
            new ErrorCode("A0402", "工序编码不存在", "ERP 工序编码在 MES 中不存在或未启用");

    /** ERP 同步：工序顺序不完整或重复 */
    public static final ErrorCode ERP_CRAFT_SEQUENCE_INVALID =
            new ErrorCode("A0420", "工序顺序不完整或重复", "工序顺序号必须从 1 开始连续且不重复");

    /** ERP 同步：待确认数据不存在 */
    public static final ErrorCode ERP_CRAFT_PENDING_NOT_EXISTS =
            new ErrorCode("A0402", "待确认工艺数据不存在", "数据不存在或已被删除");

    /** ERP 同步：待确认数据状态不允许确认 */
    public static final ErrorCode ERP_CRAFT_PENDING_STATUS_INVALID =
            new ErrorCode("A0440", "当前状态不允许确认", "只有待确认状态的数据才能确认");

    /** 设备未维护有效报工绑定 */
    public static final ErrorCode DEVICE_BINDING_NOT_AVAILABLE =
            new ErrorCode("A0402", "设备不存在或未启用", "请先维护并启用设备报工绑定");

    /** 设备绑定产线与派工产线不一致 */
    public static final ErrorCode DEVICE_BINDING_LINE_MISMATCH =
            new ErrorCode("A0440", "设备未绑定当前派工产线", "请检查设备与派工产线绑定");

    /** 设备绑定工序与上报工序不一致 */
    public static final ErrorCode DEVICE_BINDING_PROCESS_MISMATCH =
            new ErrorCode("A0440", "设备未绑定当前工序", "请检查设备与工序绑定");

    /** 设备单次计数增量异常 */
    public static final ErrorCode DEVICE_COUNT_JUMP =
            new ErrorCode("A0420", "设备计数增量超过阈值", "计数变化异常，已进入异常池");

    /** 设备绑定产线无效 */
    public static final ErrorCode DEVICE_BINDING_LINE_INVALID =
            new ErrorCode("A0402", "设备绑定产线不存在", "请选择有效产线");

    /** 设备绑定工序无效 */
    public static final ErrorCode DEVICE_BINDING_PROCESS_INVALID =
            new ErrorCode("A0402", "设备绑定工序不存在", "请选择有效工序");

    /** 设备默认员工无效 */
    public static final ErrorCode DEVICE_BINDING_EMPLOYEE_INVALID =
            new ErrorCode("A0402", "设备默认员工不存在", "请选择有效员工");

    /** 设备异常记录不存在 */
    public static final ErrorCode DEVICE_EXCEPTION_NOT_EXISTS =
            new ErrorCode("A0402", "设备计数异常不存在", "异常记录不存在或已被处理");

    /** 设备异常状态不允许处理 */
    public static final ErrorCode DEVICE_EXCEPTION_STATUS_INVALID =
            new ErrorCode("A0440", "设备计数异常已处理", "请刷新异常列表");

    /** ERP 远程接口调用失败 */
    public static final ErrorCode ERP_REMOTE_CALL_FAILED =
            new ErrorCode("C0001", "ERP 接口调用失败", "ERP 暂时不可用，请稍后重试");

    private IntegrationErrorCodeConstants() {
    }
}
