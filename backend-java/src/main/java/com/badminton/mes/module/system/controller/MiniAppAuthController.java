package com.badminton.mes.module.system.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.module.system.controller.vo.MiniAppBindByCodeReqVO;
import com.badminton.mes.module.system.controller.vo.MiniAppLoginReqVO;
import com.badminton.mes.module.system.controller.vo.MiniAppLoginRespVO;
import com.badminton.mes.module.system.controller.vo.WechatBindingCodePreviewRespVO;
import com.badminton.mes.module.system.controller.vo.WechatBindingCodeRespVO;
import com.badminton.mes.module.system.controller.vo.WechatBindingStatusRespVO;
import com.badminton.mes.module.system.service.MiniAppAuthService;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

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

    /** 为当前账号生成微信绑定小程序码。 */
    @PostMapping("/binding_codes")
    public CommonResult<WechatBindingCodeRespVO> createBindingCode() {
        return CommonResult.success(miniAppAuthService.createBindingCode());
    }

    /** 查询扫码页展示的脱敏账号信息。 */
    @GetMapping("/binding_codes/{ticket}/preview")
    public CommonResult<WechatBindingCodePreviewRespVO> previewBindingCode(
            @PathVariable("ticket")
            @Pattern(regexp = "^[0-9a-f]{32}$", message = "绑定票据格式不正确") String ticket) {
        return CommonResult.success(miniAppAuthService.previewBindingCode(ticket));
    }

    /** 查询当前账号生成的绑定票据状态。 */
    @GetMapping("/binding_codes/{ticket}/status")
    public CommonResult<WechatBindingStatusRespVO> getBindingCodeStatus(
            @PathVariable("ticket")
            @Pattern(regexp = "^[0-9a-f]{32}$", message = "绑定票据格式不正确") String ticket) {
        return CommonResult.success(miniAppAuthService.getBindingCodeStatus(ticket));
    }

    /** 扫码微信确认绑定，不接收 MES 工号或密码。 */
    @PostMapping("/bind_by_code")
    public CommonResult<WechatBindingStatusRespVO> bindByCode(
            @Valid @RequestBody MiniAppBindByCodeReqVO request) {
        return CommonResult.success(miniAppAuthService.bindByCode(request));
    }

    /** 解除当前微信身份绑定。 */
    @DeleteMapping("/unbind")
    public CommonResult<Void> unbind() {
        miniAppAuthService.unbind();
        return CommonResult.success(null);
    }
}
