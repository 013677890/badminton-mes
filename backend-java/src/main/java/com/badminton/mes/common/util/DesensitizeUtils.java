package com.badminton.mes.common.util;

import org.springframework.util.StringUtils;

/**
 * 敏感数据脱敏工具(SEC-002：用户敏感数据禁止直接展示)。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public final class DesensitizeUtils {

    private static final int MOBILE_KEEP_PREFIX = 3;

    private static final int MOBILE_KEEP_SUFFIX = 4;

    /**
     * 手机号脱敏：保留前 3 位与后 4 位，中间用 **** 遮蔽，
     * 例如 13912341219 → 139****1219；位数不足时全遮蔽。
     *
     * @param mobile 原始手机号
     * @return 脱敏后手机号，入参为空时原样返回
     */
    public static String maskMobile(String mobile) {
        if (!StringUtils.hasText(mobile)) {
            return mobile;
        }
        if (mobile.length() <= MOBILE_KEEP_PREFIX + MOBILE_KEEP_SUFFIX) {
            return "****";
        }
        return mobile.substring(0, MOBILE_KEEP_PREFIX) + "****"
                + mobile.substring(mobile.length() - MOBILE_KEEP_SUFFIX);
    }

    private DesensitizeUtils() {
    }
}
