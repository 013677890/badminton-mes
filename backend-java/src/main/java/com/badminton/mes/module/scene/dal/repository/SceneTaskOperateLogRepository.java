package com.badminton.mes.module.scene.dal.repository;
import com.badminton.mes.module.scene.dal.entity.SceneTaskOperateLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
/** 任务操作日志 Repository。 @author 刘涵 */
public interface SceneTaskOperateLogRepository extends JpaRepository<SceneTaskOperateLogEntity, Long> { }
