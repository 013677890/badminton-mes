package com.badminton.mes.module.device.constants;

import com.badminton.mes.common.core.ErrorCode;

/** 设备数据接入模块错误码。 */
public final class DeviceErrorCodeConstants {

    public static final ErrorCode ACCESS_CONFIG_NOT_EXISTS =
            new ErrorCode("A0402", "设备接入配置不存在", "设备接入配置不存在或已删除，请刷新后重试");

    public static final ErrorCode ACCESS_CONFIG_CODE_DUPLICATE =
            new ErrorCode("A0506", "设备接入配置编码已存在", "请更换接入配置编码后重试");

    public static final ErrorCode COLLECTION_POINT_DUPLICATE =
            new ErrorCode("A0506", "设备采集点已配置", "同一设备不能重复配置相同采集点");

    public static final ErrorCode ACCESS_CONFIG_ENABLE_NOT_ALLOWED =
            new ErrorCode("A0440", "设备接入配置暂不能启用", "请先完成并通过设备联调");

    public static final ErrorCode ACCESS_CONFIG_HAS_HISTORY =
            new ErrorCode("A0440", "设备接入配置存在历史数据", "请停用配置，历史联调和计数数据必须保留");

    public static final ErrorCode EQUIPMENT_NOT_AVAILABLE =
            new ErrorCode("A0440", "设备当前不可采集", "设备必须存在、启用且未报废");

    public static final ErrorCode COMMISSIONING_RECORD_NOT_EXISTS =
            new ErrorCode("A0402", "设备联调记录不存在", "设备联调记录不存在，请刷新后重试");

    public static final ErrorCode COMMISSIONING_ISSUE_REQUIRED =
            new ErrorCode("A0420", "联调失败时必须填写问题说明", "请填写通信或数据格式问题说明");

    public static final ErrorCode COMMISSIONING_TIME_INVALID =
            new ErrorCode("A0420", "设备联调时间不合法", "联调时间不能晚于当前时间");

    public static final ErrorCode COUNT_RECORD_NOT_EXISTS =
            new ErrorCode("A0402", "设备计数记录不存在", "设备计数记录不存在，请刷新后重试");

    public static final ErrorCode COUNT_REPORT_DUPLICATE =
            new ErrorCode("A0506", "设备计数数据已接收", "相同配置、采集时间和流水号的数据不能重复上报");

    public static final ErrorCode COUNT_COLLECTION_TIME_INVALID =
            new ErrorCode("A0420", "设备采集时间不合法", "设备采集时间不能晚于当前时间");

    public static final ErrorCode COUNT_REPORT_CONFIG_UNAVAILABLE =
            new ErrorCode("A0440", "设备接入配置不可采集", "配置必须启用且已通过联调");

    public static final ErrorCode COUNT_REPORT_EQUIPMENT_MISMATCH =
            new ErrorCode("A0420", "上报设备与接入配置不匹配", "请核对设备编码和接入配置编码");

    public static final ErrorCode COUNT_EXCEPTION_NOT_EXISTS =
            new ErrorCode("A0402", "设备计数异常不存在", "设备计数异常不存在，请刷新后重试");

    public static final ErrorCode COUNT_EXCEPTION_ALREADY_PROCESSED =
            new ErrorCode("A0440", "设备计数异常已处理", "已处理的异常记录不能重复处理");

    private DeviceErrorCodeConstants() {
    }
}
