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

    private IntegrationErrorCodeConstants() {
    }
}
