package com.badminton.mes.module.scene.dal.repository;
import com.badminton.mes.module.scene.dal.entity.SceneCompletionOrderEntity;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
/** 完工单 Repository。 @author 刘涵 */
public interface SceneCompletionOrderRepository extends JpaRepository<SceneCompletionOrderEntity,Long>{
 Optional<SceneCompletionOrderEntity> findByIdAndDeletedFalse(Long id);
 Optional<SceneCompletionOrderEntity> findByTaskIdAndDeletedFalse(Long taskId);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("SELECT o FROM SceneCompletionOrderEntity o WHERE o.id=:id AND o.deleted=false")
 Optional<SceneCompletionOrderEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);
}
