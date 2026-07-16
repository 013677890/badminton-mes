package com.badminton.mes.module.scene.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.scene.dal.entity.SceneExecutionTaskEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/** 派工下发与报工闭环任务 Repository。 */
public interface SceneExecutionTaskRepository extends JpaRepository<SceneExecutionTaskEntity, Long> {
    Optional<SceneExecutionTaskEntity> findByDispatchOrderIdAndDeletedFalse(Long dispatchOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT task FROM SceneExecutionTaskEntity task
            WHERE task.id = :id AND task.deleted = false
            """)
    Optional<SceneExecutionTaskEntity> findByIdForUpdate(@Param("id") Long id);
}
