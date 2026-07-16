package com.badminton.mes.module.andon.dal.repository;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.andon.dal.entity.AndonConfigurationEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 安灯异常处理配置数据访问接口。
 *
 * <p>支持配置范围唯一性校验、产线级与范围级启用规则匹配、并发维护时的悲观写锁，
 * 以及类型变更和删除保护所需的级联查询。
 */
public interface AndonConfigurationRepository extends JpaRepository<AndonConfigurationEntity, Long>,
        JpaSpecificationExecutor<AndonConfigurationEntity> {

    /** 按主键读取未逻辑删除的配置，供详情展示等只读场景使用。 */
    Optional<AndonConfigurationEntity> findByIdAndDeletedFalse(Long id);

    /** 以悲观写锁读取活动配置，串行化同一配置的更新、启停和删除操作。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select configuration from AndonConfigurationEntity configuration "
            + "where configuration.id = :id and configuration.deleted = false")
    Optional<AndonConfigurationEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /** 判断指定类型与规范化产线范围是否已有活动配置，用于新增时的范围唯一性校验。 */
    boolean existsByAndonTypeIdAndScopeLineIdAndDeletedFalse(Long andonTypeId, Long scopeLineId);

    /** 更新配置时排除自身，检查指定类型与范围是否和其他活动配置冲突。 */
    boolean existsByAndonTypeIdAndScopeLineIdAndIdNotAndDeletedFalse(
            Long andonTypeId, Long scopeLineId, Long id);

    /**
     * 按事件实际产线匹配最新一条启用配置。
     *
     * <p>按主键倒序取首条，使调用方在历史数据存在多条时获得最近维护的产线级规则。
     */
    Optional<AndonConfigurationEntity>
            findFirstByAndonTypeIdAndProductionLineIdAndEnabledStatusAndDeletedFalseOrderByIdDesc(
                    Long andonTypeId, Long productionLineId, Integer enabledStatus);

    /**
     * 按规范化范围产线匹配最新一条启用配置，供产线级或全局兜底范围查询使用。
     */
    Optional<AndonConfigurationEntity>
            findFirstByAndonTypeIdAndScopeLineIdAndEnabledStatusAndDeletedFalseOrderByIdDesc(
                    Long andonTypeId, Long scopeLineId, Integer enabledStatus);

    /**
     * 悲观锁定指定类型、业务产线下的全部活动配置。
     *
     * <p>范围维护操作先锁定现有候选，再执行启停或冲突处理，避免并发事务同时产生多条活动规则。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select configuration from AndonConfigurationEntity configuration "
            + "where configuration.andonTypeId = :andonTypeId "
            + "and configuration.productionLineId = :productionLineId "
            + "and configuration.enabledStatus = :enabledStatus "
            + "and configuration.deleted = false order by configuration.id desc")
    List<AndonConfigurationEntity> findActiveLineConfigurationsForUpdate(
            @Param("andonTypeId") Long andonTypeId,
            @Param("productionLineId") Long productionLineId,
            @Param("enabledStatus") Integer enabledStatus);

    /**
     * 悲观锁定指定类型、规范化范围下的全部活动配置，用于串行化同一配置范围的维护操作。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select configuration from AndonConfigurationEntity configuration "
            + "where configuration.andonTypeId = :andonTypeId "
            + "and configuration.scopeLineId = :scopeLineId "
            + "and configuration.enabledStatus = :enabledStatus "
            + "and configuration.deleted = false order by configuration.id desc")
    List<AndonConfigurationEntity> findActiveScopeConfigurationsForUpdate(
            @Param("andonTypeId") Long andonTypeId,
            @Param("scopeLineId") Long scopeLineId,
            @Param("enabledStatus") Integer enabledStatus);

    /** 查询指定类型下的全部未删除配置，供类型关联数据读取或维护校验使用。 */
    List<AndonConfigurationEntity> findByAndonTypeIdAndDeletedFalse(Long andonTypeId);

    /**
     * 仅查询指定类型下未删除配置的主键，供类型变更后级联失效配置详情缓存。
     */
    @Query("select configuration.id from AndonConfigurationEntity configuration "
            + "where configuration.andonTypeId = :andonTypeId and configuration.deleted = false")
    List<Long> findIdsByAndonTypeIdAndDeletedFalse(@Param("andonTypeId") Long andonTypeId);

    /**
     * 统计指定类型关联的未关闭活动事件。
     *
     * <p>存在活动事件时配置仍是事件指派和时限依据，服务层据此阻止删除相关处理规则。
     */
    @Query(value = "select count(*) from andon_event "
            + "where andon_type_id = :andonTypeId "
            + "and event_status <> 'CLOSED' and is_deleted = false", nativeQuery = true)
    long countActiveEventsByAndonTypeId(@Param("andonTypeId") Long andonTypeId);
}
