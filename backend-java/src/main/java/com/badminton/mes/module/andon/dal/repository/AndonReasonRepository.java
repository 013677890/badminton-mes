package com.badminton.mes.module.andon.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.andon.dal.entity.AndonReasonEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/** 安灯异常原因 Repository。 */
public interface AndonReasonRepository extends JpaRepository<AndonReasonEntity, Long>,
        JpaSpecificationExecutor<AndonReasonEntity> {

    Optional<AndonReasonEntity> findByIdAndDeletedFalse(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reason from AndonReasonEntity reason where reason.id = :id and reason.deleted = false")
    Optional<AndonReasonEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    boolean existsByReasonCodeAndDeletedFalse(String reasonCode);

    boolean existsByReasonCodeAndIdNotAndDeletedFalse(String reasonCode, Long id);

    boolean existsByReasonCode(String reasonCode);

    @Query(value = "select count(*) from andon_event where is_deleted = false "
            + "and (reason_id = :reasonId or actual_reason_id = :reasonId)", nativeQuery = true)
    long countEventReferences(@Param("reasonId") Long reasonId);
}
