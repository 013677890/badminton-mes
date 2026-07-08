package com.badminton.mes.common.core;

/**
 * 全局通用错误码，取值复用《Java开发手册(黄山版)》附表 3 的宏观错误码。
 *
 * <p>模块专属错误码放在各模块 {@code enums} 包的 {@code XxxErrorCodeConstants} 中，
 * 与业务代码就近维护；本类只放跨模块通用的错误码。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
public final class GlobalErrorCodeConstants {

    /** 成功。全部正常但必须填充错误码时返回五个零(ERRCODE-003) */
    public static final ErrorCode SUCCESS = new ErrorCode("00000", "成功", null);

    /** 用户请求参数错误(附表 3 二级宏观错误码 A0400)，参数校验失败统一使用 */
    public static final ErrorCode PARAM_ERROR =
            new ErrorCode("A0400", "用户请求参数错误", "请求参数有误，请检查后重试");

    /** 请求 JSON 解析失败(附表 3 A0427) */
    public static final ErrorCode JSON_PARSE_ERROR =
            new ErrorCode("A0427", "请求 JSON 解析失败", "请求格式错误，请检查提交内容");

    /** 系统执行出错(附表 3 一级宏观错误码 B0001)，未知异常兜底使用 */
    public static final ErrorCode SYSTEM_ERROR =
            new ErrorCode("B0001", "系统执行出错", "系统繁忙，请稍后重试");

    private GlobalErrorCodeConstants() {
    }
}
