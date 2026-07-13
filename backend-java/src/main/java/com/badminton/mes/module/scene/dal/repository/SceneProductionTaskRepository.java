package com.badminton.mes.module.scene.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.scene.dal.entity.SceneProductionTaskEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 现场生产任务 Repository。
 *
 * @author Codex
 * @date 2026/07/13
 */
public interface SceneProductionTaskRepository extends JpaRepository<SceneProductionTaskEntity, Long> {

    Optional<SceneProductionTaskEntity> findByDispatchOrderIdAndDeletedFalse(Long dispatchOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT task FROM SceneProductionTaskEntity task
            WHERE task.id = :id AND task.deleted = false
            """)
    Optional<SceneProductionTaskEntity> findByIdForUpdate(@Param("id") Long id);
}
