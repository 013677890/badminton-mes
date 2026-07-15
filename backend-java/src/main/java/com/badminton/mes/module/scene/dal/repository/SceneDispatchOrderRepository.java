package com.badminton.mes.module.scene.dal.repository;

import java.util.Collection;
import java.util.Optional;
import com.badminton.mes.module.scene.dal.entity.SceneDispatchOrderEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

/** 工序派工 Repository。 @author 刘涵 */
public interface SceneDispatchOrderRepository extends JpaRepository<SceneDispatchOrderEntity, Long>,
        JpaSpecificationExecutor<SceneDispatchOrderEntity> {
    Optional<SceneDispatchOrderEntity> findByIdAndDeletedFalse(Long id);
    Optional<SceneDispatchOrderEntity> findFirstByTaskIdAndDispatchStatusInAndDeletedFalse(
            Long taskId, Collection<Integer> statuses);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SceneDispatchOrderEntity d SET d.dispatchStatus=:toStatus,d.updateTime=CURRENT_TIMESTAMP WHERE d.id=:id AND d.dispatchStatus=:fromStatus AND d.deleted=false")
    int transition(@Param("id") Long id, @Param("fromStatus") Integer fromStatus, @Param("toStatus") Integer toStatus);
}
