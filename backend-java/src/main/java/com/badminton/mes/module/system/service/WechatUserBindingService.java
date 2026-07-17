package com.badminton.mes.module.system.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.badminton.mes.module.system.dal.entity.WechatUserBindingEntity;

/**
 * 微信身份与 MES 用户绑定关系服务，MySQL 绑定表是唯一事实数据源。
 *
 * @author Codex
 * @date 2026/07/16
 */
public interface WechatUserBindingService {

    /**
     * 按 OpenID 查询当前小程序的有效绑定。
     *
     * @param openId 微信 OpenID
     * @return 有效绑定
     */
    Optional<WechatUserBindingEntity> findActiveByOpenId(String openId);

    /**
     * 按用户查询当前小程序的有效绑定。
     *
     * @param userId 用户主键
     * @return 有效绑定
     */
    Optional<WechatUserBindingEntity> findActiveByUserId(Long userId);

    /**
     * 批量查询用户的有效绑定，供用户列表回填绑定状态。
     *
     * @param userIds 用户主键集合
     * @return userId 到有效绑定的映射
     */
    Map<Long, WechatUserBindingEntity> findActiveByUserIds(Collection<Long> userIds);

    /**
     * 查询当前小程序全部已绑定用户主键，供管理员筛选绑定状态。
     *
     * @return 已绑定用户主键集合
     */
    Set<Long> findActiveUserIds();

    /**
     * 建立微信身份与用户的一对一绑定。
     *
     * @param userId 用户主键
     * @param openId 微信 OpenID
     * @return 新建绑定
     */
    WechatUserBindingEntity bind(Long userId, String openId);

    /**
     * 更新微信登录时间。
     *
     * @param binding 有效绑定
     * @param loginTime 登录时间
     * @return 更新后的绑定
     */
    WechatUserBindingEntity updateLastLogin(WechatUserBindingEntity binding, LocalDateTime loginTime);

    /**
     * 解除用户当前微信绑定，并在事务提交后使该用户全部会话失效。
     *
     * @param userId 用户主键
     */
    void unbind(Long userId);
}
