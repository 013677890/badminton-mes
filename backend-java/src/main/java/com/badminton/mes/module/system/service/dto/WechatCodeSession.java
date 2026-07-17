package com.badminton.mes.module.system.service.dto;

/**
 * 微信 code2Session 返回的稳定身份信息。
 *
 * @param openId 微信 OpenID
 * @author Codex
 * @date 2026/07/15
 */
public record WechatCodeSession(String openId) {
}
