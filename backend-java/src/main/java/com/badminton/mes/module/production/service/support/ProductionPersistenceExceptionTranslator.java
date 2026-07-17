package com.badminton.mes.module.production.service.support;

import java.util.Locale;

import com.badminton.mes.common.core.ErrorCode;
import com.badminton.mes.common.exception.ServiceException;

import org.springframework.dao.DataIntegrityViolationException;

/**
 * 生产模块数据库唯一约束异常转换器。
 *
 * <p>Spring 的完整异常链可能包装多层驱动异常，本类只按约束名做精确识别；未知约束继续抛出原异常，避免错误映射。
 */
public final class ProductionPersistenceExceptionTranslator {

    /**
     * 命中指定约束时转换为业务异常，否则保留原数据库异常。
     *
     * <p>调用方可以据此把编码重复等可预期冲突返回给前端，同时不吞掉其他数据完整性错误。
     */
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
        // 约束名大小写不敏感；沿 cause 链检查是为了兼容不同数据库驱动和 Spring 异常包装层级。
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
        // 工具类不允许实例化，所有行为均通过静态方法提供。
    }
}
