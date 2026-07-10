package com.badminton.mes.module.equipment.dal.repository;

import java.util.Optional;
import java.util.Set;

import com.badminton.mes.module.equipment.dal.entity.EquipmentCategoryEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 设备类别 JPA Repository。
 *
 * @author 角色C
 * @date 2026/07/09
 */
public interface EquipmentCategoryRepository extends JpaRepository<EquipmentCategoryEntity, Long>,
        JpaSpecificationExecutor<EquipmentCategoryEntity> {

    /**
     * 按主键查询未删除的设备类别。
     *
     * @param id 类别主键
     * @return 类别实体
     */
    Optional<EquipmentCategoryEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 以写锁查询未删除设备类别，保证工序关联校验与类别停用互斥。
     *
     * @param id 类别主键
     * @return 设备类别实体
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT category FROM EquipmentCategoryEntity category "
            + "WHERE category.id = :id AND category.deleted = false")
    Optional<EquipmentCategoryEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /**
     * 判断未删除类别中是否已存在指定类别编码。
     *
     * @param categoryCode 类别编码
     * @return true 存在，false 不存在
     */
    boolean existsByCategoryCodeAndDeletedFalse(String categoryCode);

    /**
     * 判断未删除类别中是否已存在指定类别编码（排除指定 id）。
     *
     * @param categoryCode 类别编码
     * @param id           排除的类别 id
     * @return true 存在，false 不存在
     */
    boolean existsByCategoryCodeAndIdNotAndDeletedFalse(String categoryCode, Long id);

    /**
     * 统计指定父级类别下的子类别数量。
     *
     * @param parentId 父级类别 id
     * @return 子类别数量
     */
    long countByParentIdAndDeletedFalse(Long parentId);

    /**
     * 逻辑删除设备类别。
     *
     * @param id 类别主键
     * @return 影响行数；0 表示类别不存在或已删除
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE EquipmentCategoryEntity category
            SET category.deleted = true,
                category.updateTime = CURRENT_TIMESTAMP
            WHERE category.id = :id
              AND category.deleted = false
            """)
    int logicDeleteById(@Param("id") Long id);

    /**
     * 查询从指定节点出发的所有祖先节点 ID。
     *
     * <p>使用 MySQL 8.0+ 递归 CTE 向上遍历父级链，最多查询 10 层深度防止异常数据导致无限递归。
     *
     * @param startId 起始节点 ID
     * @return 祖先节点 ID 集合（不包括自己）
     */
    @Query(value = """
            WITH RECURSIVE parent_chain AS (
                SELECT parent_id, 1 as depth
                FROM equip_category
                WHERE id = :startId AND is_deleted = false AND parent_id IS NOT NULL
                
                UNION ALL
                
                SELECT c.parent_id, pc.depth + 1
                FROM equip_category c
                INNER JOIN parent_chain pc ON c.id = pc.parent_id
                WHERE c.is_deleted = false AND c.parent_id IS NOT NULL AND pc.depth < 10
            )
            SELECT DISTINCT parent_id FROM parent_chain
            """, nativeQuery = true)
    Set<Long> findAncestorIds(@Param("startId") Long startId);
}
