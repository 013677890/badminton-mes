package com.badminton.mes.module.system.service.dto;

import java.time.LocalDateTime;

/** Redis 中保存的微信绑定票据载荷。 */
public record WechatBindingTicket(
        Long userId,
        String userNo,
        String userName,
        LocalDateTime expiresAt) {
}
