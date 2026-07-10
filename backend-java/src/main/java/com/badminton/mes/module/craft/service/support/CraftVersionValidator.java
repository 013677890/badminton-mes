package com.badminton.mes.module.craft.service.support;

import java.util.Objects;

import com.badminton.mes.common.core.ErrorCode;
import com.badminton.mes.common.exception.ServiceException;

/**
 * 工艺管理客户端预期版本校验器。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public final class CraftVersionValidator {

    /**
     * 校验客户端读取时的版本与数据库当前版本一致。
     *
     * @param currentVersion  数据库当前版本
     * @param expectedVersion 客户端预期版本
     * @param errorCode       版本冲突业务错误码
     */
    public static void validate(Integer currentVersion, Integer expectedVersion, ErrorCode errorCode) {
        if (!Objects.equals(currentVersion, expectedVersion)) {
            throw new ServiceException(errorCode);
        }
    }

    private CraftVersionValidator() {
    }
}
