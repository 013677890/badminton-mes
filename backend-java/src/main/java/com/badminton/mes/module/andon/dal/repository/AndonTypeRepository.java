package com.badminton.mes.module.andon.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.andon.dal.entity.AndonTypeEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/** 安灯类型 Repository。 */
public interface AndonTypeRepository extends JpaRepository<AndonTypeEntity, Long>,
        JpaSpecificationExecutor<AndonTypeEntity> {

    Optional<AndonTypeEntity> findByIdAndDeletedFalse(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select type from AndonTypeEntity type where type.id = :id and type.deleted = false")
    Optional<AndonTypeEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    boolean existsByTypeCodeAndDeletedFalse(String typeCode);

    boolean existsByTypeCodeAndIdNotAndDeletedFalse(String typeCode, Long id);

    boolean existsByTypeCode(String typeCode);

    @Query(value = "select count(*) from andon_configuration "
            + "where andon_type_id = :typeId and is_deleted = false", nativeQuery = true)
    long countConfigurationsByTypeId(@Param("typeId") Long typeId);

    @Query(value = "select count(*) from andon_reason "
            + "where andon_type_id = :typeId and is_deleted = false", nativeQuery = true)
    long countReasonsByTypeId(@Param("typeId") Long typeId);

    @Query(value = "select count(*) from andon_event "
            + "where andon_type_id = :typeId and is_deleted = false", nativeQuery = true)
    long countEventsByTypeId(@Param("typeId") Long typeId);
}
