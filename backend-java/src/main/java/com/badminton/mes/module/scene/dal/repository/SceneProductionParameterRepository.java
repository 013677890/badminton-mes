package com.badminton.mes.module.scene.dal.repository;

import java.util.List;
import java.util.Optional;
import com.badminton.mes.module.scene.dal.entity.SceneProductionParameterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** 生产参数 Repository。 @author 刘涵 */
public interface SceneProductionParameterRepository extends JpaRepository<SceneProductionParameterEntity, Long>,
        JpaSpecificationExecutor<SceneProductionParameterEntity> {
    Optional<SceneProductionParameterEntity> findByIdAndDeletedFalse(Long id);
    boolean existsByParamCodeAndWorkshopIdAndLineIdAndProductIdAndDeletedFalse(
            String code, Long workshopId, Long lineId, Long productId);
    List<SceneProductionParameterEntity> findByParamCodeAndStatusAndDeletedFalse(String code, Integer status);
}
