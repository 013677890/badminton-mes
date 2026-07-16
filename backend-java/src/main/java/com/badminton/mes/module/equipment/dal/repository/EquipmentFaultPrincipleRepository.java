package com.badminton.mes.module.equipment.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.equipment.dal.entity.EquipmentFaultPrincipleEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 设备故障原理 JPA Repository。
 *
 * <p>提供有效字典读取、更新场景的悲观写锁、故障编码防重以及类别引用计数。所有带
 * {@code DeletedFalse} 的派生查询和显式 JPQL 均隔离逻辑删除数据，避免失效故障原理继续参与报修。
 *
 * @author 角色C
 * @date 2026/07/10
 */
public interface EquipmentFaultPrincipleRepository extends JpaRepository<EquipmentFaultPrincipleEntity, Long>,
        JpaSpecificationExecutor<EquipmentFaultPrincipleEntity> {

    /**
     * 按主键读取未逻辑删除的故障原理，供详情展示和无需锁定的业务校验使用。
     *
     * <p>派生条件固定追加 {@code deleted = false}，不申请数据库锁。
     *
     * @param id 故障原理主键
     * @return 当前有效的故障原理；不存在或已逻辑删除时为 {@link Optional#empty()}
     */
    Optional<EquipmentFaultPrincipleEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 按主键读取有效故障原理，并对命中行申请悲观写锁。
     *
     * <p>锁保持到当前事务结束，用于串行化字典修改、删除与报修引用校验，避免并发事务在
     * 校验通过后立即改变同一故障原理的可用状态。
     *
     * @param id 故障原理主键
     * @return 已锁定的有效故障原理；不存在或已逻辑删除时为 {@link Optional#empty()}
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select faultPrinciple from EquipmentFaultPrincipleEntity faultPrinciple where faultPrinciple.id = :id and faultPrinciple.deleted = false")
    Optional<EquipmentFaultPrincipleEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /**
     * 判断有效故障原理中是否已占用指定编码，供新增字典项前防重。
     *
     * <p>查询排除逻辑删除项且不加锁，数据库唯一约束负责兜底并发写入竞争。
     *
     * @param faultCode 故障编码
     * @return {@code true} 表示编码已被有效字典项占用，否则为 {@code false}
     */
    boolean existsByFaultCodeAndDeletedFalse(String faultCode);

    /**
     * 判断除当前记录外是否有其他有效故障原理占用指定编码，供修改时防重。
     *
     * <p>主键排除与 {@code deleted = false} 同时生效；查询不加锁，由数据库唯一约束兜底并发竞争。
     *
     * @param faultCode 故障编码
     * @param id        排除的故障原理 id
     * @return {@code true} 表示其他有效记录已占用编码，否则为 {@code false}
     */
    boolean existsByFaultCodeAndIdNotAndDeletedFalse(String faultCode, Long id);

    /**
     * 统计指定类别下的有效故障原理数量，供类别删除前执行引用保护。
     *
     * <p>仅统计类别主键匹配且未逻辑删除的字典项；类别为空的通用故障不计入指定类别结果。
     * 查询不加锁，仅返回聚合数量而不加载实体。
     *
     * @param categoryId 设备类别 id
     * @return 当前仍归属该类别的有效故障原理数量
     */
    long countByCategoryIdAndDeletedFalse(Long categoryId);
}
