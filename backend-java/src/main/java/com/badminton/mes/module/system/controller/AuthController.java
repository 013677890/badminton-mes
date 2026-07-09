package com.badminton.mes.module.system.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.module.system.controller.vo.AuthLoginReqVO;
import com.badminton.mes.module.system.controller.vo.AuthLoginRespVO;
import com.badminton.mes.module.system.controller.vo.AuthPasswordReqVO;
import com.badminton.mes.module.system.controller.vo.AuthProfileRespVO;
import com.badminton.mes.module.system.service.AuthService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 认证 Controller。
 *
 * <p>login 是唯一免登录接口(白名单见 SecurityWebConfig)，
 * 其余接口经拦截器登录校验后进入。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@RestController
@RequestMapping("/api/system/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param authService 认证 Service
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 工号 + 密码登录。
     *
     * @param reqVO 登录请求
     * @return token 与用户信息
     */
    @PostMapping("/login")
    public CommonResult<AuthLoginRespVO> login(@Valid @RequestBody AuthLoginReqVO reqVO) {
        return CommonResult.success(authService.login(reqVO));
    }

    /**
     * 登出，删除当前会话，幂等。
     *
     * @return 空数据成功响应
     */
    @PostMapping("/logout")
    public CommonResult<Void> logout() {
        authService.logout();
        return CommonResult.success(null);
    }

    /**
     * 查询当前登录用户信息。
     *
     * @return 当前用户信息，手机号脱敏
     */
    @GetMapping("/profile")
    public CommonResult<AuthProfileRespVO> getProfile() {
        return CommonResult.success(authService.getProfile());
    }

    /**
     * 修改本人密码，成功后当前会话失效，需重新登录。
     *
     * @param reqVO 修改密码请求
     * @return 空数据成功响应
     */
    @PutMapping("/password")
    public CommonResult<Void> changePassword(@Valid @RequestBody AuthPasswordReqVO reqVO) {
        authService.changePassword(reqVO);
        return CommonResult.success(null);
    }
}
