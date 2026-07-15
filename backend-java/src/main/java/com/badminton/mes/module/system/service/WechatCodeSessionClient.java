package com.badminton.mes.module.system.service;

import com.badminton.mes.module.system.service.dto.WechatCodeSession;

/**
 * 微信小程序 code2Session 客户端。
 *
 * @author Codex
 * @date 2026/07/15
 */
public interface WechatCodeSessionClient {

    /**
     * 使用 wx.login 临时 code 换取 OpenID。
     *
     * @param code 临时 code
     * @return 微信身份
     */
    WechatCodeSession exchange(String code);
}
