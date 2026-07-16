package com.badminton.mes.module.system.service;

import com.badminton.mes.module.system.controller.vo.MiniAppBindByCodeReqVO;
import com.badminton.mes.module.system.controller.vo.MiniAppLoginReqVO;
import com.badminton.mes.module.system.controller.vo.MiniAppLoginRespVO;
import com.badminton.mes.module.system.controller.vo.WechatBindingCodePreviewRespVO;
import com.badminton.mes.module.system.controller.vo.WechatBindingCodeRespVO;
import com.badminton.mes.module.system.controller.vo.WechatBindingStatusRespVO;

/**
 * 微信小程序认证服务。
 *
 * @author Codex
 * @date 2026/07/15
 */
public interface MiniAppAuthService {

    /**
     * 使用微信临时 code 登录。
     *
     * @param request 登录请求
     * @return 登录结果或绑定票据
     */
    MiniAppLoginRespVO login(MiniAppLoginReqVO request);

    /** 为当前账号生成短期一次性微信绑定小程序码。 */
    WechatBindingCodeRespVO createBindingCode();

    /** 查询扫码页面所需的脱敏账号预览。 */
    WechatBindingCodePreviewRespVO previewBindingCode(String ticket);

    /** 查询当前账号生成的绑定票据状态。 */
    WechatBindingStatusRespVO getBindingCodeStatus(String ticket);

    /** 使用扫码微信的临时 code 消费票据并建立绑定。 */
    WechatBindingStatusRespVO bindByCode(MiniAppBindByCodeReqVO request);

    /** 解除当前登录用户的小程序绑定。 */
    void unbind();
}
