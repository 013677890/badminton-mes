package com.badminton.mes.module.system.controller.vo;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 用户响应 VO，详情与分页列表共用。
 *
 * <p>不出参密码等敏感字段；手机号脱敏返回(SEC-002)；
 * 时间统一 yyyy-MM-dd HH:mm:ss(API-013)。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
public class UserRespVO {

    /** 用户主键 */
    private Long id;

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

    /** 状态：1 启用 0 停用 */
    private Integer status;

    /** 角色 id 列表，编辑回显用 */
    private List<Long> roleIds;

    /** 角色编码列表 */
    private List<String> roleCodes;

    /** 角色名称列表，与 roleIds 顺序一致，供展示 */
    private List<String> roleNames;

    /** 是否已绑定当前微信小程序。 */
    private boolean wechatBound;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
