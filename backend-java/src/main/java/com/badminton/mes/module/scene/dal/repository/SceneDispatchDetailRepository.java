package com.badminton.mes.module.scene.dal.repository;

import java.util.List;
import java.util.Optional;
import com.badminton.mes.module.scene.dal.entity.SceneDispatchDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** 工序派工明细 Repository。 @author 刘涵 */
public interface SceneDispatchDetailRepository extends JpaRepository<SceneDispatchDetailEntity, Long> {
    Optional<SceneDispatchDetailEntity> findByIdAndDeletedFalse(Long id);
    List<SceneDispatchDetailEntity> findByDispatchIdAndDeletedFalseOrderBySeqAsc(Long dispatchId);
    List<SceneDispatchDetailEntity> findByTaskIdAndDeletedFalseOrderBySeqAsc(Long taskId);
    long countByDispatchIdAndDetailStatusAndDeletedFalse(Long dispatchId, Integer status);
}
