package com.badminton.mes.module.system.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.module.system.controller.vo.MiniAppBindReqVO;
import com.badminton.mes.module.system.controller.vo.MiniAppLoginReqVO;
import com.badminton.mes.module.system.controller.vo.MiniAppLoginRespVO;
import com.badminton.mes.module.system.service.MiniAppAuthService;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 微信小程序登录与 MES 工号绑定接口。
 *
 * @author Codex
 * @date 2026/07/15
 */
@Validated
@RestController
@RequestMapping("/api/system/mini_app/auth")
public class MiniAppAuthController {

    private final MiniAppAuthService miniAppAuthService;

    /**
     * 构造小程序认证 Controller。
     *
     * @param miniAppAuthService 小程序认证服务
     */
    public MiniAppAuthController(MiniAppAuthService miniAppAuthService) {
        this.miniAppAuthService = miniAppAuthService;
    }

    /**
     * 微信临时 code 登录。
     *
     * @param request 登录请求
     * @return 登录结果或绑定票据
     */
    @PostMapping("/login")
    public CommonResult<MiniAppLoginRespVO> login(@Valid @RequestBody MiniAppLoginReqVO request) {
        return CommonResult.success(miniAppAuthService.login(request));
    }

    /**
     * 首次绑定 MES 工号。
     *
     * @param request 绑定请求
     * @return 登录结果
     */
    @PostMapping("/bind")
    public CommonResult<MiniAppLoginRespVO> bind(@Valid @RequestBody MiniAppBindReqVO request) {
        return CommonResult.success(miniAppAuthService.bind(request));
    }

    /** 解除当前微信身份绑定。 */
    @DeleteMapping("/unbind")
    public CommonResult<Void> unbind() {
        miniAppAuthService.unbind();
        return CommonResult.success(null);
    }
}
