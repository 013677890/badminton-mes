package com.badminton.mes.common.core;

import lombok.Data;

/**
 * 统一响应结构。
 *
 * <p>按黄山版前后端规约 API-003，出错响应必须包含 HTTP 状态码、errorCode、
 * errorMessage、用户提示四部分：HTTP 状态码由 {@code GlobalExceptionHandler}
 * 按错误码来源映射，其余三部分由本类承载。JSON key 均为 lowerCamelCase(API-004)。
 *
 * @param <T> 业务数据类型
 * @author 张竹灏
 * @date 2026/07/07
 */
@Data
public class CommonResult<T> {

    /** 错误码，5 位字符串，成功固定为 00000 */
    private String code;

    /** 错误描述，供前端与排查人员定位问题(errorMessage) */
    private String message;

    /** 用户提示，面向最终用户展示，成功时为 null */
    private String userTip;

    /** 业务数据，出错时为 null */
    private T data;

    /**
     * 构造成功响应。
     *
     * @param data 业务数据，列表类接口为空时应传空集合而非 null(API-002)
     * @param <T>  业务数据类型
     * @return 成功响应
     */
    public static <T> CommonResult<T> success(T data) {
        CommonResult<T> result = new CommonResult<>();
        result.setCode(GlobalErrorCodeConstants.SUCCESS.code());
        result.setMessage(GlobalErrorCodeConstants.SUCCESS.message());
        result.setData(data);
        return result;
    }

    /**
     * 构造失败响应。
     *
     * @param errorCode 错误码对象
     * @param <T>       业务数据类型
     * @return 失败响应，data 为 null
     */
    public static <T> CommonResult<T> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.message());
    }

    /**
     * 构造失败响应，并用更具体的描述覆盖错误码默认 message。
     *
     * @param errorCode 错误码对象
     * @param message   具体错误描述，例如逐条拼接的参数校验失败原因
     * @param <T>       业务数据类型
     * @return 失败响应，data 为 null
     */
    public static <T> CommonResult<T> error(ErrorCode errorCode, String message) {
        CommonResult<T> result = new CommonResult<>();
        result.setCode(errorCode.code());
        result.setMessage(message);
        result.setUserTip(errorCode.userTip());
        return result;
    }
}
