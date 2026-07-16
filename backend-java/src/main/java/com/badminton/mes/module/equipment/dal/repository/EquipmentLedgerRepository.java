package com.badminton.mes.module.equipment.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.equipment.dal.entity.EquipmentLedgerEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 设备台账 JPA Repository。
 *
 * <p>除基础 CRUD 和动态规格分页外，集中提供有效台账读取、悲观写锁读取、编码防重及类别、
 * 制造商引用计数。名称中带 {@code DeletedFalse} 的派生查询均排除逻辑删除数据；全表编码查询
 * 则有意包含已删除记录，用于生成不会碰撞数据库唯一键的保留编码。
 *
 * @author 角色C
 * @date 2026/07/09
 */
public interface EquipmentLedgerRepository extends JpaRepository<EquipmentLedgerEntity, Long>,
        JpaSpecificationExecutor<EquipmentLedgerEntity> {

    /**
     * 按主键读取未逻辑删除的设备台账，供无需串行化的详情展示和只读校验使用。
     *
     * <p>查询固定过滤 {@code deleted = false} 且不加锁；不存在或已删除时返回空值。
     *
     * @param id 设备主键
     * @return 当前有效的设备台账；不存在或已逻辑删除时为 {@link Optional#empty()}
     */
    Optional<EquipmentLedgerEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 按主键读取有效设备台账，并对命中行申请悲观写锁。
     *
     * <p>JPQL 固定过滤逻辑删除数据；锁持续到当前事务结束，用于串行化设备状态变更、报修和
     * 保养等并发操作，防止多个事务基于同一旧状态同时决策。
     *
     * @param id 设备主键
     * @return 已锁定的有效台账；不存在或已逻辑删除时为 {@link Optional#empty()}
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ledger from EquipmentLedgerEntity ledger where ledger.id = :id and ledger.deleted = false")
    Optional<EquipmentLedgerEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /**
     * 判断有效台账中是否已占用指定设备编码，供新增设备前执行防重校验。
     *
     * <p>派生条件排除逻辑删除记录且不加锁，数据库唯一约束仍负责兜底并发写入竞争。
     *
     * @param equipmentCode 设备编码
     * @return {@code true} 表示编码已被有效台账占用，否则为 {@code false}
     */
    boolean existsByEquipmentCodeAndDeletedFalse(String equipmentCode);

    /**
     * 判断除当前设备外是否有其他有效台账占用指定编码，供修改设备编码时防重。
     *
     * <p>主键排除与 {@code deleted = false} 同时生效；查询不加锁，由数据库唯一约束兜底并发竞争。
     *
     * @param equipmentCode 设备编码
     * @param id            排除的设备 id
     * @return {@code true} 表示其他有效台账已占用编码，否则为 {@code false}
     */
    boolean existsByEquipmentCodeAndIdNotAndDeletedFalse(String equipmentCode, Long id);

    /**
     * 在包含逻辑删除记录的全表范围检查设备编码是否存在。
     *
     * <p>该查询故意不追加删除过滤且不加锁，供逻辑删除时生成保留编码并规避数据库唯一键碰撞。
     *
     * @param equipmentCode 待检查的设备编码
     * @return {@code true} 表示任意历史或有效记录使用过该编码，否则为 {@code false}
     */
    boolean existsByEquipmentCode(String equipmentCode);

    /**
     * 统计指定类别下的有效设备数量，供删除或停用类别前执行引用保护。
     *
     * <p>仅计入 {@code categoryId} 匹配且 {@code deleted = false} 的台账，不加锁也不加载实体列表。
     *
     * @param categoryId 设备类别 id
     * @return 当前仍引用该类别的有效设备数量
     */
    long countByCategoryIdAndDeletedFalse(Long categoryId);

    /**
     * 统计指定制造商下的有效设备数量，供删除制造商前执行引用保护。
     *
     * <p>仅计入 {@code manufacturerId} 匹配且 {@code deleted = false} 的台账，不加锁且不加载实体。
     *
     * @param manufacturerId 设备制造商 id
     * @return 当前仍引用该制造商的有效设备数量
     */
    long countByManufacturerIdAndDeletedFalse(Long manufacturerId);
}
