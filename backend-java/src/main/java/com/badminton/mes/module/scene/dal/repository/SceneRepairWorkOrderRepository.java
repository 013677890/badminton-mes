package com.badminton.mes.module.scene.dal.repository;

import com.badminton.mes.module.scene.dal.entity.SceneRepairWorkOrderEntity;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 返修工单 Repository。 @author 刘涵 */
public interface SceneRepairWorkOrderRepository extends JpaRepository<SceneRepairWorkOrderEntity, Long> {
    Optional<SceneRepairWorkOrderEntity> findByIdAndDeletedFalse(Long id);
    Optional<SceneRepairWorkOrderEntity> findBySourceReportIdAndDeletedFalse(Long sourceReportId);
    List<SceneRepairWorkOrderEntity> findByBatchNoAndDeletedFalseOrderByCreatedTimeAsc(String batchNo);
    List<SceneRepairWorkOrderEntity> findAllByDeletedFalseOrderByCreatedTimeAsc();
}
