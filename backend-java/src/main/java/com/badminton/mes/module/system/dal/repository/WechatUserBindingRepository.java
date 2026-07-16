package com.badminton.mes.module.system.dal.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.system.dal.entity.WechatUserBindingEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 微信小程序用户绑定 Repository。
 *
 * @author Codex
 * @date 2026/07/15
 */
public interface WechatUserBindingRepository extends JpaRepository<WechatUserBindingEntity, Long> {

    /**
     * 按小程序和 OpenID 查询有效绑定。
     *
     * @param appId 小程序 AppID
     * @param openId 微信 OpenID
     * @return 有效绑定
     */
    Optional<WechatUserBindingEntity> findByAppIdAndOpenIdAndStatusAndDeletedFalse(
            String appId, String openId, Integer status);

    /**
     * 按小程序和用户查询有效绑定。
     *
     * @param appId 小程序 AppID
     * @param userId 用户主键
     * @param status 状态
     * @return 有效绑定
     */
    Optional<WechatUserBindingEntity> findByAppIdAndUserIdAndStatusAndDeletedFalse(
            String appId, Long userId, Integer status);

    /** 批量查询指定用户的有效绑定。 */
    List<WechatUserBindingEntity> findByAppIdAndUserIdInAndStatusAndDeletedFalse(
            String appId, Collection<Long> userIds, Integer status);

    /** 查询当前小程序全部有效绑定。 */
    List<WechatUserBindingEntity> findByAppIdAndStatusAndDeletedFalse(String appId, Integer status);
}
