package com.badminton.mes.module.system.service;

import com.badminton.mes.module.system.controller.vo.MiniAppBindReqVO;
import com.badminton.mes.module.system.controller.vo.MiniAppLoginReqVO;
import com.badminton.mes.module.system.controller.vo.MiniAppLoginRespVO;

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

    /**
     * 首次绑定微信身份与 MES 工号。
     *
     * @param request 绑定请求
     * @return 登录结果
     */
    MiniAppLoginRespVO bind(MiniAppBindReqVO request);

    /** 解除当前登录用户的小程序绑定。 */
    void unbind();
}
