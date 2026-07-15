package com.badminton.mes.module.device.constants;

import com.badminton.mes.common.core.ErrorCode;

/**
 * 设备数据接入模块错误码定义。
 *
 * <p>集中描述接入配置、联调、计数上报和异常处理各阶段可被客户端识别的失败原因。
 * 错误码对象同时携带面向操作人员的提示，业务层应复用这些常量，避免不同入口对同一约束返回不一致语义。
 */
public final class DeviceErrorCodeConstants {

    /** 查询目标配置不存在，或配置已被逻辑删除而不再参与设备接入。 */
    public static final ErrorCode ACCESS_CONFIG_NOT_EXISTS =
            new ErrorCode("A0402", "设备接入配置不存在", "设备接入配置不存在或已删除，请刷新后重试");

    /** 接入配置编码违反业务唯一性或数据库唯一约束。 */
    public static final ErrorCode ACCESS_CONFIG_CODE_DUPLICATE =
            new ErrorCode("A0506", "设备接入配置编码已存在", "请更换接入配置编码后重试");

    /** 同一设备下的采集点编码重复，无法唯一定位数据来源。 */
    public static final ErrorCode COLLECTION_POINT_DUPLICATE =
            new ErrorCode("A0506", "设备采集点已配置", "同一设备不能重复配置相同采集点");

    /** 配置尚未通过联调，禁止进入正式采集状态。 */
    public static final ErrorCode ACCESS_CONFIG_ENABLE_NOT_ALLOWED =
            new ErrorCode("A0440", "设备接入配置暂不能启用", "请先完成并通过设备联调");

    /** 配置已产生联调或计数历史，只允许停用而不允许删除业务档案。 */
    public static final ErrorCode ACCESS_CONFIG_HAS_HISTORY =
            new ErrorCode("A0440", "设备接入配置存在历史数据", "请停用配置，历史联调和计数数据必须保留");

    /** 关联设备台账处于停用或报废状态，不能建立或更新采集关系。 */
    public static final ErrorCode EQUIPMENT_NOT_AVAILABLE =
            new ErrorCode("A0440", "设备当前不可采集", "设备必须存在、启用且未报废");

    /** 指定联调记录不存在。 */
    public static final ErrorCode COMMISSIONING_RECORD_NOT_EXISTS =
            new ErrorCode("A0402", "设备联调记录不存在", "设备联调记录不存在，请刷新后重试");

    /** 联调任一检查失败时缺少可供排障追踪的问题说明。 */
    public static final ErrorCode COMMISSIONING_ISSUE_REQUIRED =
            new ErrorCode("A0420", "联调失败时必须填写问题说明", "请填写通信或数据格式问题说明");

    /** 联调时间晚于服务器当前时间，无法作为可信的历史测试事实。 */
    public static final ErrorCode COMMISSIONING_TIME_INVALID =
            new ErrorCode("A0420", "设备联调时间不合法", "联调时间不能晚于当前时间");

    /** 指定设备计数原始记录不存在。 */
    public static final ErrorCode COUNT_RECORD_NOT_EXISTS =
            new ErrorCode("A0402", "设备计数记录不存在", "设备计数记录不存在，请刷新后重试");

    /** 业务预检或数据库唯一约束确认本次计数报文已经接收。 */
    public static final ErrorCode COUNT_REPORT_DUPLICATE =
            new ErrorCode("A0506", "设备计数数据已接收", "相同配置、采集时间和流水号的数据不能重复上报");

    /** 设备采集时间晚于服务器当前时间，禁止写入未来数据。 */
    public static final ErrorCode COUNT_COLLECTION_TIME_INVALID =
            new ErrorCode("A0420", "设备采集时间不合法", "设备采集时间不能晚于当前时间");

    /** 接入配置未启用或未通过联调，不满足正式接收计数的前置条件。 */
    public static final ErrorCode COUNT_REPORT_CONFIG_UNAVAILABLE =
            new ErrorCode("A0440", "设备接入配置不可采集", "配置必须启用且已通过联调");

    /** 报文中的设备编码与配置绑定的台账设备不一致。 */
    public static final ErrorCode COUNT_REPORT_EQUIPMENT_MISMATCH =
            new ErrorCode("A0420", "上报设备与接入配置不匹配", "请核对设备编码和接入配置编码");

    /** 指定计数异常记录不存在。 */
    public static final ErrorCode COUNT_EXCEPTION_NOT_EXISTS =
            new ErrorCode("A0402", "设备计数异常不存在", "设备计数异常不存在，请刷新后重试");

    /** 异常已结束待处理状态，阻止重复处置覆盖首次处理结论。 */
    public static final ErrorCode COUNT_EXCEPTION_ALREADY_PROCESSED =
            new ErrorCode("A0440", "设备计数异常已处理", "已处理的异常记录不能重复处理");

    /** 工具类不允许实例化。 */
    private DeviceErrorCodeConstants() {
    }
}
