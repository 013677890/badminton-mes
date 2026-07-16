package com.badminton.mes.module.quality.dal.repository;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.quality.dal.entity.QualityInspectionResultEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 质量检验项目结果快照持久化仓库。
 *
 * <p>结果行从属于检验单聚合，既保存创建任务时冻结的项目规则，也保存后续实测值和判定结果；查询时
 * 始终通过检验单主键限定聚合边界，避免跨单据读取或更新结果。
 */
public interface QualityInspectionResultRepository extends JpaRepository<QualityInspectionResultEntity, Long> {

    /** 按方案顺序和主键稳定读取检验单的全部结果快照，供录入、完整性校验及详情展示使用。 */
    List<QualityInspectionResultEntity> findByInspectionRecordIdOrderBySortOrderAscIdAsc(Long inspectionRecordId);

    /** 按结果主键及所属检验单联合定位，校验客户端提交的结果确实属于当前单据。 */
    Optional<QualityInspectionResultEntity> findByIdAndInspectionRecordId(Long id, Long inspectionRecordId);
}
