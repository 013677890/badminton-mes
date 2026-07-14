package com.badminton.mes.module.system.service.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.module.system.dal.entity.UserEntity;
import com.badminton.mes.module.system.dal.repository.UserRepository;
import com.badminton.mes.module.system.service.OrganizationUserReferenceQuery;

import org.springframework.stereotype.Service;

/**
 * 车间与产线的系统用户反向引用查询实现。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@Service
public class OrganizationUserReferenceQueryImpl implements OrganizationUserReferenceQuery {

    private final UserRepository userRepository;

    /**
     * 构造用户引用查询服务。
     *
     * @param userRepository 用户 Repository
     */
    public OrganizationUserReferenceQueryImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean isEnabledUser(Long userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
                .filter(user -> CommonStatusEnum.ENABLED.getStatus().equals(user.getStatus()))
                .isPresent();
    }

    @Override
    public boolean hasAnyWorkshopUser(Long workshopId) {
        return userRepository.existsByWorkshopIdAndDeletedFalse(workshopId);
    }

    @Override
    public boolean hasEnabledWorkshopUser(Long workshopId) {
        return userRepository.existsByWorkshopIdAndStatusAndDeletedFalse(
                workshopId, CommonStatusEnum.ENABLED.getStatus());
    }

    @Override
    public boolean hasAnyProductionLineUser(Long lineId) {
        return userRepository.existsByLineIdAndDeletedFalse(lineId);
    }

    @Override
    public boolean hasEnabledProductionLineUser(Long lineId) {
        return userRepository.existsByLineIdAndStatusAndDeletedFalse(
                lineId, CommonStatusEnum.ENABLED.getStatus());
    }

    @Override
    public Map<Long, String> loadUserNames(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRepository.findByIdInAndDeletedFalse(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getUserName));
    }
}
