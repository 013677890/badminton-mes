package com.badminton.mes.module.production.service.support;

import java.util.Locale;

import com.badminton.mes.common.core.ErrorCode;
import com.badminton.mes.common.exception.ServiceException;

import org.springframework.dao.DataIntegrityViolationException;

/** 生产模块数据库唯一约束异常转换器。 */
public final class ProductionPersistenceExceptionTranslator {

    /** 命中指定约束时转换为业务异常，否则保留原数据库异常。 */
    public static void translateUniqueConstraint(DataIntegrityViolationException exception,
                                                 String constraintName, ErrorCode errorCode) {
        if (isConstraintViolation(exception, constraintName)) {
            throw new ServiceException(errorCode);
        }
        throw exception;
    }

    /**
     * 判断异常链是否命中指定数据库约束。
     *
     * @param exception      数据完整性异常
     * @param constraintName 约束名
     * @return true 表示命中
     */
    public static boolean isConstraintViolation(DataIntegrityViolationException exception,
                                                String constraintName) {
        Throwable cause = exception;
        String expected = constraintName.toLowerCase(Locale.ROOT);
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains(expected)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private ProductionPersistenceExceptionTranslator() {
    }
}
