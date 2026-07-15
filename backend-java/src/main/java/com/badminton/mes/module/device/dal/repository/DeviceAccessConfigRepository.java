package com.badminton.mes.module.device.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.device.dal.entity.DeviceAccessConfigEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 设备接入配置 Repository。
 *
 * <p>普通详情和存在性查询均不加锁；带 {@code ForUpdate} 的方法在事务内使用悲观写锁，
 * 用于启停、联调状态更新和通信时间推进等需要串行校验并修改同一配置的场景。
 */
public interface DeviceAccessConfigRepository extends JpaRepository<DeviceAccessConfigEntity, Long>,
        JpaSpecificationExecutor<DeviceAccessConfigEntity> {

    /** 按主键查询未逻辑删除的配置，不加锁，用于只读详情和业务存在性校验。 */
    Optional<DeviceAccessConfigEntity> findByIdAndDeletedFalse(Long id);

    /** 按对外配置编码查询未逻辑删除记录，不加锁，用于上报入口定位有效接入规则。 */
    Optional<DeviceAccessConfigEntity> findByConfigCodeAndDeletedFalse(String configCode);

    /**
     * 按主键查询未删除配置并获取悲观写锁。
     *
     * <p>{@code deleted = false} 排除历史逻辑删除记录；写锁用于事务内先校验当前状态再更新，
     * 防止并发启停、联调或删除操作相互覆盖。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select config from DeviceAccessConfigEntity config "
            + "where config.id = :id and config.deleted = false")
    Optional<DeviceAccessConfigEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /**
     * 按配置编码查询未删除配置并获取悲观写锁。
     *
     * <p>供设备上报等以编码定位配置且需要原子推进状态的事务使用；逻辑删除数据不会被命中，
     * 同一配置上的并发写入会等待当前事务释放锁。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select config from DeviceAccessConfigEntity config "
            + "where config.configCode = :configCode and config.deleted = false")
    Optional<DeviceAccessConfigEntity> findByConfigCodeAndDeletedFalseForUpdate(
            @Param("configCode") String configCode);

    /** 检查配置编码是否已被未删除记录占用，不加锁，用于新增前的友好重复提示。 */
    boolean existsByConfigCodeAndDeletedFalse(String configCode);

    /** 排除当前主键后检查未删除配置编码是否重复，不加锁，用于编辑时唯一性预检。 */
    boolean existsByConfigCodeAndIdNotAndDeletedFalse(String configCode, Long id);

    /** 检查配置编码是否曾被任何记录使用，包含逻辑删除数据，用于保护全历史编码唯一性。 */
    boolean existsByConfigCode(String configCode);

    /** 检查未删除配置中“设备 + 采集点”映射是否存在，不加锁，用于新增时避免来源歧义。 */
    boolean existsByEquipmentIdAndCollectionPointCodeAndDeletedFalse(Long equipmentId, String collectionPointCode);

    /** 排除当前主键后检查未删除的设备采集点映射是否重复，用于编辑配置时的唯一性预检。 */
    boolean existsByEquipmentIdAndCollectionPointCodeAndIdNotAndDeletedFalse(
            Long equipmentId, String collectionPointCode, Long id);
}
