package com.badminton.mes.module.equipment.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.equipment.dal.entity.EquipmentMaintenanceRecordEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 设备保养记录 JPA Repository。
 *
 * <p>除普通查询外，提供状态机更新所需的悲观锁、并发任务计数和最近完成时间聚合。所有派生查询
 * 均显式包含逻辑删除条件，防止已删除待处理任务继续影响计划和设备状态判断。
 *
 * @author 角色C
 * @date 2026/07/11
 */
public interface EquipmentMaintenanceRecordRepository extends JpaRepository<EquipmentMaintenanceRecordEntity, Long>,
        JpaSpecificationExecutor<EquipmentMaintenanceRecordEntity> {

    /**
     * 按主键查询有效记录，不申请数据库锁，供只读详情使用。
     *
     * @param id 保养记录主键
     * @return 有效记录；不存在或已逻辑删除时为 {@link Optional#empty()}
     */
    Optional<EquipmentMaintenanceRecordEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 锁定待更新记录，防止两个请求同时从同一旧状态发起不同迁移。
     *
     * <p>JPQL 固定过滤 {@code deleted = false}；悲观写锁保持到当前事务结束，供修改、开始、
     * 完成、取消和删除流程在同一任务上串行执行。
     *
     * @param id 保养记录主键
     * @return 锁定的有效记录，不存在或已逻辑删除时为空
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select record from EquipmentMaintenanceRecordEntity record where record.id = :id and record.deleted = false")
    Optional<EquipmentMaintenanceRecordEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /**
     * 判断有效记录中任务编号是否已占用，供新增任务前防重。
     *
     * <p>查询排除逻辑删除记录且不加锁，数据库唯一约束负责最终并发防重。
     *
     * @param recordNo 待检查的任务编号
     * @return {@code true} 表示编号已被有效记录占用，否则为 {@code false}
     */
    boolean existsByRecordNoAndDeletedFalse(String recordNo);

    /**
     * 修改时排除当前记录，判断编号是否被其他有效保养任务占用。
     *
     * <p>主键排除与 {@code deleted = false} 同时生效；查询不加锁，由数据库唯一约束兜底并发竞争。
     *
     * @param recordNo 待检查的新任务编号
     * @param id       当前记录主键，不参与重复判断
     * @return {@code true} 表示其他有效任务已占用编号，否则为 {@code false}
     */
    boolean existsByRecordNoAndIdNotAndDeletedFalse(String recordNo, Long id);

    /**
     * 在包含逻辑删除记录的全表范围检查保留编号是否碰撞。
     *
     * <p>该查询有意不追加删除过滤且不加锁，供逻辑删除时改写业务编号并规避历史唯一键冲突。
     *
     * @param recordNo 待检查的系统生成保留编号
     * @return {@code true} 表示任意有效或历史记录已使用该编号，否则为 {@code false}
     */
    boolean existsByRecordNo(String recordNo);

    /**
     * 统计计划下所有有效记录，用于禁止删除计划或变更其绑定设备。
     *
     * <p>不区分任务状态，所有未逻辑删除的历史和进行中记录均计入；查询不加锁且不加载实体。
     *
     * @param planId 保养计划主键
     * @return 当前仍关联该计划的有效记录数量
     */
    long countByPlanIdAndDeletedFalse(Long planId);

    /**
     * 统计计划下处于指定状态集合的有效记录，供判断计划是否存在待办或执行中任务。
     *
     * <p>计划、状态集合和 {@code deleted = false} 同时生效；查询不加锁，仅返回聚合数量。
     *
     * @param planId        保养计划主键
     * @param recordStatuses 待统计的任务状态集合
     * @return 同时匹配计划、状态集合且未逻辑删除的记录数量
     */
    long countByPlanIdAndRecordStatusInAndDeletedFalse(Long planId, Iterable<String> recordStatuses);

    /**
     * 统计设备下处于指定状态集合的有效记录，供设备状态切换和并发任务约束使用。
     *
     * <p>设备、状态集合和 {@code deleted = false} 同时生效；查询不加锁，仅返回聚合数量。
     *
     * @param equipmentId   设备台账主键
     * @param recordStatuses 待统计的任务状态集合
     * @return 同时匹配设备、状态集合且未逻辑删除的记录数量
     */
    long countByEquipmentIdAndRecordStatusInAndDeletedFalse(Long equipmentId, Iterable<String> recordStatuses);

    /**
     * 统计设备的全部有效保养历史，用于设备删除前的引用保护。
     *
     * <p>不区分任务状态，已完成和已取消但未逻辑删除的历史记录也会计入；查询不加锁。
     *
     * @param equipmentId 设备台账主键
     * @return 当前仍关联该设备的有效保养记录数量
     */
    long countByEquipmentIdAndDeletedFalse(Long equipmentId);

    /**
     * 统计同一设备除当前任务外的指定状态记录，用于阻止并发保养及安全恢复设备状态。
     *
     * <p>设备、单一状态、排除主键和逻辑删除过滤同时生效；仅返回聚合数量，不锁定匹配记录。
     *
     * @param equipmentId 设备台账主键
     * @param recordStatus 待检查的单一任务状态
     * @param id            当前保养记录主键，不参与计数
     * @return 其他有效任务中处于指定状态的记录数量
     */
    long countByEquipmentIdAndRecordStatusAndIdNotAndDeletedFalse(
            Long equipmentId, String recordStatus, Long id);

    /**
     * 查询计划下最近一次已完成任务的完成时间，以数据库聚合结果作为周期计算基准。
     *
     * <p>JPQL 固定筛选 {@code recordStatus = COMPLETED} 和 {@code deleted = false}，通过
     * {@code max(finishTime)} 避免加载全部历史；查询不加锁，调用方应在计划写锁事务内使用结果。
     *
     * @param planId 保养计划主键
     * @return 最近完成时间；尚无已完成记录时为空
     */
    @Query("select max(record.finishTime) from EquipmentMaintenanceRecordEntity record "
            + "where record.planId = :planId and record.recordStatus = 'COMPLETED' and record.deleted = false")
    Optional<java.time.LocalDateTime> findLatestCompletedTimeByPlanId(@Param("planId") Long planId);
}
