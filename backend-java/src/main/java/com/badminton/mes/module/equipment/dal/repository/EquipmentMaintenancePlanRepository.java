package com.badminton.mes.module.equipment.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.equipment.dal.entity.EquipmentMaintenancePlanEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 设备保养计划 JPA Repository。
 *
 * <p>同时提供派生查询、动态规格分页和写锁查询。业务更新使用悲观写锁读取计划，以串行化同一计划
 * 上的任务创建、计划修改和完成时间回写，避免并发事务基于过期计划状态做决定。
 *
 * @author 角色C
 * @date 2026/07/11
 */
public interface EquipmentMaintenancePlanRepository extends JpaRepository<EquipmentMaintenancePlanEntity, Long>,
        JpaSpecificationExecutor<EquipmentMaintenancePlanEntity> {

    /**
     * 按主键查询当前有效计划，不加锁，供只读详情使用。
     *
     * @param id 保养计划主键
     * @return 有效计划；不存在或已逻辑删除时为 {@link Optional#empty()}
     */
    Optional<EquipmentMaintenancePlanEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 按主键查询当前有效计划并申请悲观写锁。
     *
     * <p>JPQL 固定过滤 {@code deleted = false}；命中行锁保持到当前事务结束，供计划修改、
     * 任务创建和完成时间回写在同一计划上串行执行。
     *
     * @param id 计划主键
     * @return 锁定的有效计划，不存在或已逻辑删除时为空
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select plan from EquipmentMaintenancePlanEntity plan where plan.id = :id and plan.deleted = false")
    Optional<EquipmentMaintenancePlanEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /**
     * 判断有效数据中是否已占用计划编码，供新增计划前防重。
     *
     * <p>派生查询固定过滤逻辑删除数据且不加锁，数据库唯一约束负责最终并发防重。
     *
     * @param planCode 待检查的计划编码
     * @return {@code true} 表示编码已被有效计划占用，否则为 {@code false}
     */
    boolean existsByPlanCodeAndDeletedFalse(String planCode);

    /**
     * 修改时排除当前计划，判断新编码是否被其他有效计划占用。
     *
     * <p>主键排除与 {@code deleted = false} 同时生效；查询不加锁，由数据库唯一约束兜底并发竞争。
     *
     * @param planCode 待检查的新计划编码
     * @param id       当前计划主键，不参与重复判断
     * @return {@code true} 表示其他有效计划已占用编码，否则为 {@code false}
     */
    boolean existsByPlanCodeAndIdNotAndDeletedFalse(String planCode, Long id);

    /**
     * 在包含逻辑删除数据的全表范围内判断系统生成编码是否碰撞。
     *
     * <p>该查询有意不附加删除过滤且不加锁，用于生成逻辑删除保留编码时避开历史唯一键。
     *
     * @param planCode 待检查的系统生成编码
     * @return {@code true} 表示任意有效或历史记录已使用该编码，否则为 {@code false}
     */
    boolean existsByPlanCode(String planCode);

    /**
     * 统计设备下指定启停状态的有效计划数量。
     *
     * <p>设备、状态及 {@code deleted = false} 同时匹配才计数；查询不加锁也不加载实体，
     * 供判断设备是否仍受启用计划约束。
     *
     * @param equipmentId 设备台账主键
     * @param status      待统计的计划启停状态
     * @return 指定设备和状态下的有效计划数量
     */
    long countByEquipmentIdAndStatusAndDeletedFalse(Long equipmentId, Integer status);

    /**
     * 统计设备下全部有效计划，用于设备删除前的历史引用保护。
     *
     * <p>不区分计划启停状态，仅排除逻辑删除数据；查询不加锁，返回聚合数量而不加载计划实体。
     *
     * @param equipmentId 设备台账主键
     * @return 当前仍关联该设备的有效计划数量
     */
    long countByEquipmentIdAndDeletedFalse(Long equipmentId);
}
