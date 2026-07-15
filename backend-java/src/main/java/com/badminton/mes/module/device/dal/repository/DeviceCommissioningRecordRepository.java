package com.badminton.mes.module.device.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.device.dal.entity.DeviceCommissioningRecordEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 设备联调记录 Repository。
 *
 * <p>联调记录作为历史事实只进行普通无锁读取；分页条件由对应 Specification 组合，
 * 关联数量查询用于判断接入配置是否已形成不可删除的联调历史。
 */
public interface DeviceCommissioningRecordRepository
        extends JpaRepository<DeviceCommissioningRecordEntity, Long>,
                JpaSpecificationExecutor<DeviceCommissioningRecordEntity> {

    /** 按主键无锁查询联调历史详情，不附加状态过滤，用于详情展示和不存在校验。 */
    Optional<DeviceCommissioningRecordEntity> findById(Long id);

    /** 按接入配置统计全部联调记录，不加锁，用于删除配置前判断是否存在历史依赖。 */
    long countByAccessConfigId(Long accessConfigId);
}
