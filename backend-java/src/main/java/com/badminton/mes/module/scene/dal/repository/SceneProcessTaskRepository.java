package com.badminton.mes.module.scene.dal.repository;

import java.util.List;

import com.badminton.mes.module.scene.dal.entity.SceneProcessTaskEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 现场工序任务 Repository。
 *
 * @author Codex
 * @date 2026/07/13
 */
public interface SceneProcessTaskRepository extends JpaRepository<SceneProcessTaskEntity, Long> {

    List<SceneProcessTaskEntity> findByProductionTaskIdAndDeletedFalseOrderBySequenceNoAsc(
            Long productionTaskId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT task FROM SceneProcessTaskEntity task
            WHERE task.productionTaskId = :productionTaskId
              AND task.processId = :processId
              AND task.deleted = false
            """)
    java.util.Optional<SceneProcessTaskEntity> findByTaskAndProcessForUpdate(
            @Param("productionTaskId") Long productionTaskId,
            @Param("processId") Long processId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT task FROM SceneProcessTaskEntity task
            WHERE task.productionTaskId = :productionTaskId AND task.deleted = false
            ORDER BY task.sequenceNo ASC
            """)
    List<SceneProcessTaskEntity> findByProductionTaskIdForUpdate(
            @Param("productionTaskId") Long productionTaskId);
}
