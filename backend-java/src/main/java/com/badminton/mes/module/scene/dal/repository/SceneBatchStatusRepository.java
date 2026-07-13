package com.badminton.mes.module.scene.dal.repository;

import java.util.Optional;
import com.badminton.mes.module.scene.dal.entity.SceneBatchStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** 产品当前状态 Repository。 @author 刘涵 */
public interface SceneBatchStatusRepository extends JpaRepository<SceneBatchStatusEntity, Long>,
        JpaSpecificationExecutor<SceneBatchStatusEntity> {
    Optional<SceneBatchStatusEntity> findByIdAndDeletedFalse(Long id);
    Optional<SceneBatchStatusEntity> findByBatchNoAndDeletedFalse(String batchNo);
}
