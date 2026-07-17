package com.badminton.mes.module.quality.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.quality.dal.entity.QualityInspectionRecordEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 质量检验单持久化仓库。
 *
 * <p>常规详情只读取未逻辑删除单据；结果录入、单据提交等写流程通过悲观写锁串行处理同一检验单，
 * 使草稿状态检查、项目结果更新与最终结论提交保持一致。
 */
public interface QualityInspectionRecordRepository extends JpaRepository<QualityInspectionRecordEntity, Long>,
        JpaSpecificationExecutor<QualityInspectionRecordEntity> {

    /** 按主键读取未删除检验单，供详情查询和无写入的业务校验使用。 */
    Optional<QualityInspectionRecordEntity> findByIdAndDeletedFalse(Long id);

    /** 对未删除检验单施加悲观写锁，防止并发录入或重复提交覆盖状态与结论。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select record from QualityInspectionRecordEntity record "
            + "where record.id = :id and record.deleted = false")
    Optional<QualityInspectionRecordEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);
}
