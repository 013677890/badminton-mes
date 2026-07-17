package com.badminton.mes.module.system.controller.vo;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 登录响应 VO：token 与前端菜单控制所需的最小用户信息。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
public class AuthLoginRespVO {

    /** 登录令牌，后续请求放入 Authorization: Bearer {token} */
    private String token;

    /** 用户主键 */
    private Long userId;

    /** 工号 */
    private String userNo;

    /** 姓名 */
    private String userName;

    /** 角色编码列表，前端按此控制菜单显隐 */
    private List<String> roleCodes;

    /** 当前账号是否已绑定微信。 */
    private boolean wechatBound;

    /** 微信绑定时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime wechatBindingTime;

    /** 微信最近登录时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime wechatLastLoginTime;
}
