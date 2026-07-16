package com.badminton.mes.module.system.service;

/** 微信小程序码客户端。 */
public interface WechatMiniAppCodeClient {

    /** 生成携带一次性绑定票据的小程序码 PNG。 */
    byte[] generateBindingCode(String ticket);
}
