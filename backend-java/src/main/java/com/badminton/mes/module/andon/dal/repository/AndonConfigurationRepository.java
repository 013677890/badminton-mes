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

/** 安灯异常处理配置 Repository。 */
public interface AndonConfigurationRepository extends JpaRepository<AndonConfigurationEntity, Long>,
        JpaSpecificationExecutor<AndonConfigurationEntity> {

    Optional<AndonConfigurationEntity> findByIdAndDeletedFalse(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select configuration from AndonConfigurationEntity configuration "
            + "where configuration.id = :id and configuration.deleted = false")
    Optional<AndonConfigurationEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    boolean existsByAndonTypeIdAndScopeLineIdAndDeletedFalse(Long andonTypeId, Long scopeLineId);

    boolean existsByAndonTypeIdAndScopeLineIdAndIdNotAndDeletedFalse(
            Long andonTypeId, Long scopeLineId, Long id);

    Optional<AndonConfigurationEntity>
            findFirstByAndonTypeIdAndProductionLineIdAndEnabledStatusAndDeletedFalseOrderByIdDesc(
                    Long andonTypeId, Long productionLineId, Integer enabledStatus);

    Optional<AndonConfigurationEntity>
            findFirstByAndonTypeIdAndScopeLineIdAndEnabledStatusAndDeletedFalseOrderByIdDesc(
                    Long andonTypeId, Long scopeLineId, Integer enabledStatus);

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

    List<AndonConfigurationEntity> findByAndonTypeIdAndDeletedFalse(Long andonTypeId);

    @Query(value = "select count(*) from andon_event "
            + "where andon_type_id = :andonTypeId "
            + "and event_status <> 'CLOSED' and is_deleted = false", nativeQuery = true)
    long countActiveEventsByAndonTypeId(@Param("andonTypeId") Long andonTypeId);
}
