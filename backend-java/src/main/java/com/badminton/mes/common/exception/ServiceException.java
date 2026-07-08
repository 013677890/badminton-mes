package com.badminton.mes.common.exception;

import com.badminton.mes.common.core.ErrorCode;

import lombok.Getter;

/**
 * 业务异常，Service 层业务规则校验不通过时抛出。
 *
 * <p>应用内部推荐通过异常表达业务错误(EXC-013)，由
 * {@code GlobalExceptionHandler} 统一转换为带错误码的响应，
 * 禁止用 null 返回值表达业务错误。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
@Getter
public class ServiceException extends RuntimeException {

    /** 错误码对象，异常与错误码一一对应 */
    private final transient ErrorCode errorCode;

    /**
     * 按错误码构造业务异常，异常信息取错误码默认 message。
     *
     * @param errorCode 错误码对象
     */
    public ServiceException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    /**
     * 按错误码构造业务异常，并携带更具体的现场描述。
     *
     * @param errorCode 错误码对象
     * @param message   具体错误描述，会覆盖错误码默认 message
     */
    public ServiceException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
