package com.badminton.mes.module.quality.dal.repository;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.quality.dal.entity.QualityInspectionPlanEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 检验标准方案版本持久化仓库。
 *
 * <p>每个实体主键定位一个不可覆盖的方案版本；版本派生和默认方案审核需要先锁定相应版本集合，
 * 再完成版本号或适用范围唯一性校验，避免并发事务生成重复版本或多个默认生效方案。
 */
public interface QualityInspectionPlanRepository extends JpaRepository<QualityInspectionPlanEntity, Long>,
        JpaSpecificationExecutor<QualityInspectionPlanEntity> {

    /** 按版本主键读取未删除方案，供详情展示和检验单创建时引用。 */
    Optional<QualityInspectionPlanEntity> findByIdAndDeletedFalse(Long id);

    /** 锁定单个未删除方案版本，串行执行草稿编辑、审核、停用和删除状态迁移。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select plan from QualityInspectionPlanEntity plan "
            + "where plan.id = :id and plan.deleted = false")
    Optional<QualityInspectionPlanEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /** 校验同一方案编码下的有效版本号是否重复。 */
    boolean existsByPlanCodeAndVersionNoAndDeletedFalse(String planCode, Integer versionNo);

    /**
     * 按版本号顺序锁定同一方案编码的全部未删除版本。
     *
     * <p>创建新版本前锁定整条版本链，使“读取最大版本号并递增”的过程在并发事务间串行。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select plan from QualityInspectionPlanEntity plan "
            + "where plan.planCode = :planCode and plan.deleted = false order by plan.versionNo")
    List<QualityInspectionPlanEntity> lockPlansByPlanCode(@Param("planCode") String planCode);

    /**
     * 锁定同一检验类型下的全部未删除方案。
     *
     * <p>审核默认方案前建立统一的加锁顺序，并串行执行产品、客户、检验类型适用范围的默认唯一性检查。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select plan from QualityInspectionPlanEntity plan "
            + "where plan.inspectionType = :inspectionType and plan.deleted = false order by plan.id")
    List<QualityInspectionPlanEntity> lockPlansByInspectionType(
            @Param("inspectionType") String inspectionType);

    /**
     * 判断指定适用范围是否已有其他默认生效版本。
     *
     * <p>产品和客户参数采用 null-safe 等值语义，{@code excludedId} 排除当前待审核版本；调用前应先
     * 完成同检验类型版本锁定，以使检查结果可用于并发安全的审核决策。
     */
    @Query("select count(plan) > 0 from QualityInspectionPlanEntity plan "
            + "where plan.deleted = false and plan.planStatus = 'EFFECTIVE' and plan.defaultFlag = true "
            + "and ((:productId is null and plan.productId is null) or plan.productId = :productId) "
            + "and ((:customerId is null and plan.customerId is null) or plan.customerId = :customerId) "
            + "and plan.inspectionType = :inspectionType and plan.id <> :excludedId")
    boolean existsEffectiveDefaultForScope(@Param("productId") Long productId,
                                           @Param("customerId") Long customerId,
                                           @Param("inspectionType") String inspectionType,
                                           @Param("excludedId") Long excludedId);
}
