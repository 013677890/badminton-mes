package com.badminton.mes.module.system.service.dto;

import java.time.LocalDateTime;

/** Redis 中保存的微信绑定状态载荷。 */
public record WechatBindingResult(
        Long userId,
        String status,
        LocalDateTime expiresAt,
        LocalDateTime bindingTime) {
}
