package com.badminton.mes.module.scene.dal.repository;

import java.util.List;
import com.badminton.mes.module.scene.dal.entity.SceneParameterChangeLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** 参数变更日志 Repository。 @author 刘涵 */
public interface SceneParameterChangeLogRepository extends JpaRepository<SceneParameterChangeLogEntity, Long> {
    List<SceneParameterChangeLogEntity> findByParamIdAndDeletedFalseOrderByOperateTimeDescIdDesc(Long paramId);
}
