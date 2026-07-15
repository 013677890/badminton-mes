package com.badminton.mes.module.scene.dal.repository;
import com.badminton.mes.module.scene.dal.entity.SceneWorkReportEntity;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
/** 报工 Repository。 @author 刘涵 */
public interface SceneWorkReportRepository extends JpaRepository<SceneWorkReportEntity,Long>{
 Optional<SceneWorkReportEntity> findByIdAndDeletedFalse(Long id);
 Optional<SceneWorkReportEntity> findByRequestNoAndDeletedFalse(String requestNo);
 boolean existsBySourceReportIdAndDeletedFalse(Long sourceReportId);
 List<SceneWorkReportEntity> findByTaskIdAndDeletedFalse(Long taskId);
}
