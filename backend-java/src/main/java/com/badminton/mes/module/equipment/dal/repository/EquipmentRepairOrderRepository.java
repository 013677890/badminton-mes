package com.badminton.mes.module.equipment.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.equipment.dal.entity.EquipmentRepairOrderEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 设备报修任务 JPA Repository。
 *
 * <p>继承基础 CRUD 和动态规格分页能力，并提供有效任务读取、单号防重及设备、故障原理引用计数。
 * 本接口没有显式锁查询；状态流转若需串行化，应由业务层锁定相关设备或通过其他事务约束完成。
 * 所有派生查询均显式排除逻辑删除任务。
 *
 * @author 角色C
 * @date 2026/07/10
 */
public interface EquipmentRepairOrderRepository extends JpaRepository<EquipmentRepairOrderEntity, Long>,
        JpaSpecificationExecutor<EquipmentRepairOrderEntity> {

    /**
     * 按主键读取未逻辑删除的报修任务，供详情展示和业务校验使用。
     *
     * <p>查询不加锁；不存在或已删除时返回空值。
     *
     * @param id 报修任务主键
     * @return 当前有效的报修任务；不存在或已逻辑删除时为 {@link Optional#empty()}
     */
    Optional<EquipmentRepairOrderEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 判断有效报修任务中是否已占用指定单号，供新增任务前防重。
     *
     * <p>查询排除逻辑删除记录且不加锁，数据库唯一约束负责最终并发防重。
     *
     * @param repairNo 报修单号
     * @return {@code true} 表示单号已被有效任务占用，否则为 {@code false}
     */
    boolean existsByRepairNoAndDeletedFalse(String repairNo);

    /**
     * 判断除当前任务外是否有其他有效报修任务占用指定单号，供修改时防重。
     *
     * <p>主键排除与 {@code deleted = false} 同时生效；查询不加锁，由数据库唯一约束兜底并发竞争。
     *
     * @param repairNo 报修单号
     * @param id       排除的报修任务 id
     * @return {@code true} 表示其他有效任务已占用单号，否则为 {@code false}
     */
    boolean existsByRepairNoAndIdNotAndDeletedFalse(String repairNo, Long id);

    /**
     * 统计指定设备下的全部有效报修任务，供设备删除前执行历史引用保护。
     *
     * <p>不区分任务状态，只排除逻辑删除记录，因此已完成和已取消的有效历史也会计入；查询不加锁。
     *
     * @param equipmentId 设备台账 id
     * @return 当前仍关联该设备的有效报修任务数量
     */
    long countByEquipmentIdAndDeletedFalse(Long equipmentId);

    /**
     * 统计指定设备下处于给定状态集合的有效报修任务。
     *
     * <p>设备主键、状态集合和 {@code deleted = false} 同时匹配才计数，供判断设备是否仍有
     * 报修流程占用；查询不加锁，仅返回聚合数量而不加载任务实体。
     *
     * @param equipmentId    设备台账 id
     * @param repairStatuses 处理中状态集合
     * @return 该设备在指定状态集合中的有效任务数量
     */
    long countByEquipmentIdAndRepairStatusInAndDeletedFalse(Long equipmentId, Iterable<String> repairStatuses);

    /**
     * 统计指定故障原理下的有效报修任务，供删除故障原理前执行历史引用保护。
     *
     * <p>不区分报修状态，所有未逻辑删除且引用该故障原理的任务均计入；查询不加锁且不加载实体。
     *
     * @param faultPrincipleId 故障原理 id
     * @return 当前仍引用该故障原理的有效报修任务数量
     */
    long countByFaultPrincipleIdAndDeletedFalse(Long faultPrincipleId);
}
