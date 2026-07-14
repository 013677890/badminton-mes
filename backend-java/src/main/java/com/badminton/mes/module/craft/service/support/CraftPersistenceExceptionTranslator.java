package com.badminton.mes.module.craft.service.support;

import java.util.Locale;

import com.badminton.mes.common.core.ErrorCode;
import com.badminton.mes.common.exception.ServiceException;

import org.springframework.dao.DataIntegrityViolationException;

/**
 * 工艺管理数据库约束异常转换器，只转换可明确识别的唯一约束冲突。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public final class CraftPersistenceExceptionTranslator {

    /**
     * 命中指定约束时转为业务异常，否则保留原始数据库异常交由全局异常处理。
     *
     * @param exception      数据完整性异常
     * @param constraintName 唯一约束名称
     * @param errorCode      对应业务错误码
     */
    public static void translateUniqueConstraint(DataIntegrityViolationException exception,
                                                 String constraintName, ErrorCode errorCode) {
        Throwable cause = exception;
        String expectedName = constraintName.toLowerCase(Locale.ROOT);
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains(expectedName)) {
                throw new ServiceException(errorCode);
            }
            cause = cause.getCause();
        }
        throw exception;
    }

    private CraftPersistenceExceptionTranslator() {
    }
}
