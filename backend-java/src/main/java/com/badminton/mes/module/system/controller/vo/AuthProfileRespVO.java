package com.badminton.mes.module.system.controller.vo;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 当前登录用户信息响应 VO，前端刷新后恢复上下文使用。
 *
 * <p>实时读库而非回放会话：管理员调整过角色/姓名时前端能看到最新值；
 * 手机号脱敏返回(SEC-002)。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
public class AuthProfileRespVO {

    /** 用户主键 */
    private Long userId;

    /** 工号 */
    private String userNo;

    /** 姓名 */
    private String userName;

    /** 手机号(脱敏) */
    private String mobile;

    /** 所属车间 id */
    private Long workshopId;

    /** 所属产线 id */
    private Long lineId;

    /** 角色编码列表 */
    private List<String> roleCodes;

    /** 角色名称列表，与 roleCodes 顺序一致，供展示 */
    private List<String> roleNames;

    /** 当前账号是否已绑定微信。 */
    private boolean wechatBound;

    /** 微信绑定时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime wechatBindingTime;

    /** 微信最近登录时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime wechatLastLoginTime;
}
