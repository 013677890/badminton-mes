package com.badminton.mes.module.andon.dal.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.andon.dal.entity.AndonEventEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/** 现场安灯异常 Repository。 */
public interface AndonEventRepository extends JpaRepository<AndonEventEntity, Long>,
        JpaSpecificationExecutor<AndonEventEntity> {

    Optional<AndonEventEntity> findByIdAndDeletedFalse(Long id);

    Optional<AndonEventEntity> findByEventNoAndDeletedFalse(String eventNo);

    boolean existsByEventNo(String eventNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from AndonEventEntity event "
            + "where event.id = :id and event.deleted = false")
    Optional<AndonEventEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    @Query("select event from AndonEventEntity event "
            + "where event.deleted = false "
            + "and event.eventStatus <> 'CLOSED' "
            + "and ((event.responseDeadline is not null and event.responseDeadline <= :now) "
            + "or (event.escalationDeadline is not null and event.escalationDeadline <= :now))")
    List<AndonEventEntity> findTimeoutCandidates(@Param("now") LocalDateTime now);
}
