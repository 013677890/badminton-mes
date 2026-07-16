package com.badminton.mes.module.scene.dal.repository;

import java.util.Collection;
import java.util.Optional;
import com.badminton.mes.module.scene.dal.entity.SceneProductionTaskEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/** 生产任务 Repository，状态流转采用 CAS。 @author 刘涵 */
public interface SceneProductionTaskRepository extends JpaRepository<SceneProductionTaskEntity, Long>,
        JpaSpecificationExecutor<SceneProductionTaskEntity> {
    Optional<SceneProductionTaskEntity> findByIdAndDeletedFalse(Long id);
    Optional<SceneProductionTaskEntity> findByDispatchOrderIdAndDeletedFalse(Long dispatchOrderId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM SceneProductionTaskEntity t WHERE t.id=:id AND t.deleted=false")
    Optional<SceneProductionTaskEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);
    boolean existsByTaskNoAndDeletedFalse(String taskNo);
    @Query("SELECT COALESCE(SUM(t.planQuantity),0) FROM SceneProductionTaskEntity t WHERE t.workOrderId=:workOrderId AND t.taskStatus NOT IN :excluded AND t.deleted=false")
    Long sumAllocated(@Param("workOrderId") Long workOrderId, @Param("excluded") Collection<Integer> excluded);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SceneProductionTaskEntity t SET t.taskStatus=:toStatus,t.pauseReason=:reason,t.updateTime=CURRENT_TIMESTAMP WHERE t.id=:id AND t.taskStatus=:fromStatus AND t.deleted=false")
    int transition(@Param("id") Long id, @Param("fromStatus") Integer fromStatus,
                   @Param("toStatus") Integer toStatus, @Param("reason") String reason);
}
