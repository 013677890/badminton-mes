package com.badminton.mes.module.system.constants;

/**
 * 微信绑定票据状态常量。
 *
 * @author Codex
 * @date 2026/07/16
 */
public final class WechatBindingStatusConstants {

    /** 等待微信扫码确认。 */
    public static final String PENDING = "PENDING";

    /** 已成功绑定。 */
    public static final String BOUND = "BOUND";

    /** 票据已过期或已被失败尝试消费。 */
    public static final String EXPIRED = "EXPIRED";

    private WechatBindingStatusConstants() {
    }
}
