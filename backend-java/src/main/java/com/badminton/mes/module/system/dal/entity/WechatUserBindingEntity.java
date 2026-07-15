package com.badminton.mes.module.system.dal.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 微信小程序身份与 MES 用户绑定实体。
 *
 * @author Codex
 * @date 2026/07/15
 */
@Data
@Entity
@DynamicInsert
@Table(name = "sys_wechat_user_binding")
public class WechatUserBindingEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** MES 用户主键 */
    @Column(name = "user_id")
    private Long userId;

    /** 微信小程序 AppID */
    @Column(name = "app_id")
    private String appId;

    /** 微信小程序 OpenID */
    @Column(name = "open_id")
    private String openId;

    /** 状态：1 启用，0 停用 */
    @Column(name = "status", columnDefinition = "tinyint unsigned")
    private Integer status;

    /** 最近登录时间 */
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    /** 创建时间 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time", insertable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @Column(name = "is_deleted", columnDefinition = "tinyint unsigned")
    private Boolean deleted;
}
