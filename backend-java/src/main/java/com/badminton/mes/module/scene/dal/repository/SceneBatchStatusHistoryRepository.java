package com.badminton.mes.module.scene.dal.repository;

import java.util.List;
import com.badminton.mes.module.scene.dal.entity.SceneBatchStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** 产品状态履历 Repository。 @author 刘涵 */
public interface SceneBatchStatusHistoryRepository extends JpaRepository<SceneBatchStatusHistoryEntity, Long> {
    List<SceneBatchStatusHistoryEntity> findByBatchStatusIdAndDeletedFalseOrderByOperateTimeDescIdDesc(Long id);
}
