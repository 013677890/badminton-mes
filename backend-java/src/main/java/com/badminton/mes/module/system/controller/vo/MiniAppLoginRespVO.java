package com.badminton.mes.module.system.controller.vo;

import java.util.List;

import lombok.Data;

/**
 * 微信小程序登录响应。
 *
 * @author Codex
 * @date 2026/07/15
 */
@Data
public class MiniAppLoginRespVO {

    /** 是否需要首次绑定 MES 工号 */
    private boolean bindingRequired;

    /** 短期绑定票据，仅未绑定时返回 */
    private String bindTicket;

    /** Bearer token，仅登录成功时返回 */
    private String token;

    /** 用户主键 */
    private Long userId;

    /** 工号 */
    private String userNo;

    /** 姓名 */
    private String userName;

    /** 角色编码 */
    private List<String> roleCodes = List.of();

    /** 所属车间 */
    private Long workshopId;

    /** 所属产线 */
    private Long lineId;
}
