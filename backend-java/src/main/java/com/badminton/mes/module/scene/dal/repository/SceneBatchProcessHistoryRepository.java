package com.badminton.mes.module.scene.dal.repository;

import java.util.List;
import com.badminton.mes.module.scene.dal.entity.SceneBatchProcessHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** 产品工序履历 Repository。 @author 刘涵 */
public interface SceneBatchProcessHistoryRepository extends JpaRepository<SceneBatchProcessHistoryEntity, Long> {
    List<SceneBatchProcessHistoryEntity> findByBatchStatusIdAndDeletedFalseOrderByOperateTimeDescIdDesc(Long id);
    boolean existsByDispatchDetailIdAndActionTypeAndDeletedFalse(Long detailId, Integer actionType);
}
