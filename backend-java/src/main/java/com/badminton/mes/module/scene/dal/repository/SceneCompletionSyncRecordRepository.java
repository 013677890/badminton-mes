package com.badminton.mes.module.scene.dal.repository;
import com.badminton.mes.module.scene.dal.entity.SceneCompletionSyncRecordEntity;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
/** 完工同步记录 Repository。 @author 刘涵 */
public interface SceneCompletionSyncRecordRepository extends JpaRepository<SceneCompletionSyncRecordEntity,Long>{
 Optional<SceneCompletionSyncRecordEntity> findByFinishOrderIdAndTargetSystemAndDeletedFalse(Long id,String target);
 List<SceneCompletionSyncRecordEntity> findByFinishOrderIdAndDeletedFalseOrderByLastSyncTimeDesc(Long id);
}
