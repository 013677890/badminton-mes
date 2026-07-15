package com.badminton.mes.module.scene.dal.repository;

import com.badminton.mes.module.scene.dal.entity.SceneRepairRecheckRecordEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 返修复检记录 Repository。 @author 刘涵 */
public interface SceneRepairRecheckRecordRepository extends JpaRepository<SceneRepairRecheckRecordEntity, Long> {
    List<SceneRepairRecheckRecordEntity> findByRepairWorkOrderIdOrderByCreatedTimeAsc(Long repairWorkOrderId);
}
