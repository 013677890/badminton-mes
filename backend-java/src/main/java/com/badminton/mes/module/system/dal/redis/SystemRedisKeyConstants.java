package com.badminton.mes.module.system.dal.redis;

import java.time.Duration;

/**
 * 系统模块 Redis Key 常量，集中管理 Key 格式与 TTL。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public final class SystemRedisKeyConstants {

    /** 登录会话正查 Key 模板，参数为 token，值为 LoginUser JSON */
    public static final String LOGIN_TOKEN = "mes:system:login:token:%s";

    /** 登录会话反向索引 Key 模板，参数为用户 id，值为该用户当前 token(单设备登录) */
    public static final String LOGIN_USER_INDEX = "mes:system:login:user:%d";

    /** 会话 TTL，拦截器命中且剩余不足一半时滑动续期 */
    public static final Duration LOGIN_SESSION_TTL = Duration.ofHours(8);

    /** 登录失败计数 Key 模板，参数为工号 */
    public static final String LOGIN_FAIL = "mes:system:login:fail:%s";

    /** 登录失败锁定窗口，自最后一次失败起算 */
    public static final Duration LOGIN_FAIL_TTL = Duration.ofMinutes(15);

    /** 登录失败锁定阈值：连续失败达到该次数后锁定 */
    public static final int LOGIN_FAIL_MAX = 5;

    /** 微信小程序绑定票据 Key 模板，参数为随机票据 */
    public static final String MINI_APP_BIND_TICKET = "mes:system:mini-app:bind-ticket:%s";

    /**
     * 构造会话正查 Key。
     *
     * @param token 登录令牌
     * @return 会话 Key
     */
    public static String loginTokenKey(String token) {
        return String.format(LOGIN_TOKEN, token);
    }

    /**
     * 构造会话反向索引 Key。
     *
     * @param userId 用户主键
     * @return 反向索引 Key
     */
    public static String loginUserIndexKey(Long userId) {
        return String.format(LOGIN_USER_INDEX, userId);
    }

    /**
     * 构造登录失败计数 Key。
     *
     * @param userNo 工号
     * @return 失败计数 Key
     */
    public static String loginFailKey(String userNo) {
        return String.format(LOGIN_FAIL, userNo);
    }

    /**
     * 构造微信小程序绑定票据 Key。
     *
     * @param ticket 随机票据
     * @return Redis Key
     */
    public static String miniAppBindTicketKey(String ticket) {
        return String.format(MINI_APP_BIND_TICKET, ticket);
    }

    private SystemRedisKeyConstants() {
    }
}
