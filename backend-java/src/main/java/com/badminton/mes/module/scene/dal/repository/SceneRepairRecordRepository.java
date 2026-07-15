package com.badminton.mes.module.scene.dal.repository;

import com.badminton.mes.module.scene.dal.entity.SceneRepairRecordEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 返修记录 Repository。 @author 刘涵 */
public interface SceneRepairRecordRepository extends JpaRepository<SceneRepairRecordEntity, Long> {
    List<SceneRepairRecordEntity> findByRepairWorkOrderIdOrderByCreatedTimeAsc(Long repairWorkOrderId);
}
