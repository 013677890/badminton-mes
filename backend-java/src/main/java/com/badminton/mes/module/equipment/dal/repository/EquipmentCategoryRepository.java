package com.badminton.mes.module.equipment.dal.repository;

import java.util.Collection;
import java.util.List;
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
 * <p>集中提供类别有效性读取、确定顺序的悲观写锁、编码与层级校验、逻辑删除更新及祖先链查询。
 * 常规派生查询和显式 JPQL 均过滤逻辑删除数据；原生递归查询仅用于读取祖先主键，不加载实体关系。
 *
 * @author 角色C
 * @date 2026/07/09
 */
public interface EquipmentCategoryRepository extends JpaRepository<EquipmentCategoryEntity, Long>,
        JpaSpecificationExecutor<EquipmentCategoryEntity> {

    /**
     * 按主键读取未逻辑删除的设备类别，供详情展示和无需串行化的只读校验使用。
     *
     * <p>派生查询固定过滤 {@code deleted = false}，不申请数据库锁。
     *
     * @param id 类别主键
     * @return 当前有效类别；不存在或已逻辑删除时为 {@link Optional#empty()}
     */
    Optional<EquipmentCategoryEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 以悲观写锁查询有效设备类别，保证工序关联校验、类别修改和停用操作互斥。
     *
     * <p>JPQL 固定排除逻辑删除记录；命中行锁保持到当前事务结束，防止并发事务在引用校验后
     * 改变同一类别状态。
     *
     * @param id 类别主键
     * @return 已锁定的有效类别；不存在或已逻辑删除时为 {@link Optional#empty()}
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT category FROM EquipmentCategoryEntity category "
            + "WHERE category.id = :id AND category.deleted = false")
    Optional<EquipmentCategoryEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /**
     * 批量查询指定状态且未逻辑删除的设备类别，供批量引用有效性校验。
     *
     * <p>仅返回主键位于集合内且状态匹配的有效实体，不加锁；调用方可通过返回数量或主键集合
     * 判断请求中的类别是否全部可用。
     *
     * @param ids    设备类别主键集合
     * @param status 启用状态
     * @return 满足主键、状态和未删除条件的类别列表；不保证覆盖所有输入主键
     */
    List<EquipmentCategoryEntity> findByIdInAndStatusAndDeletedFalse(
            Collection<Long> ids, Integer status);

    /**
     * 按主键升序悲观写锁指定状态的有效类别，供路线审核在锁内复核引用。
     *
     * <p>JPQL 同时过滤主键集合、启停状态和逻辑删除标记；固定升序获取锁可让并发审核遵循
     * 一致加锁顺序，降低多类别批量锁定时发生死锁的概率。锁保持到当前事务结束。
     *
     * @param ids    设备类别主键集合
     * @param status 启用状态
     * @return 按主键升序排列的已锁定有效类别；缺失或不可用主键不会出现在结果中
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT category FROM EquipmentCategoryEntity category
            WHERE category.id IN :ids
              AND category.status = :status
              AND category.deleted = false
            ORDER BY category.id ASC
            """)
    List<EquipmentCategoryEntity> findAvailableByIdInForUpdateOrderByIdAsc(
            @Param("ids") Collection<Long> ids, @Param("status") Integer status);

    /**
     * 判断有效类别中是否已占用指定编码，供新增类别前防重。
     *
     * <p>查询排除逻辑删除类别且不加锁，数据库唯一约束负责最终并发防重。
     *
     * @param categoryCode 类别编码
     * @return {@code true} 表示编码已被有效类别占用，否则为 {@code false}
     */
    boolean existsByCategoryCodeAndDeletedFalse(String categoryCode);

    /**
     * 判断除当前类别外是否有其他有效类别占用指定编码，供修改时防重。
     *
     * <p>查询同时应用主键排除和 {@code deleted = false} 条件，不申请数据库锁。
     *
     * @param categoryCode 类别编码
     * @param id           排除的类别 id
     * @return {@code true} 表示其他有效类别已占用编码，否则为 {@code false}
     */
    boolean existsByCategoryCodeAndIdNotAndDeletedFalse(String categoryCode, Long id);

    /**
     * 统计指定父类别下未逻辑删除的直接子类别数量，供删除前执行层级引用保护。
     *
     * <p>该派生查询只检查一层直接子节点，不递归统计更深层后代，不加锁也不加载实体。
     *
     * @param parentId 父级类别 id
     * @return 当前有效的直接子类别数量
     */
    long countByParentIdAndDeletedFalse(Long parentId);

    /**
     * 以条件更新方式逻辑删除设备类别，并同步刷新数据库更新时间。
     *
     * <p>仅当主键匹配且记录当前未删除时执行更新；{@code clearAutomatically} 清除持久化上下文，
     * {@code flushAutomatically} 在更新前刷新待写数据，避免后续读取命中一级缓存中的旧实体。
     * 条件更新会由数据库锁定命中行直至事务结束，返回值供调用方识别并发删除或目标缺失。
     *
     * @param id 类别主键
     * @return 影响行数；{@code 1} 表示删除成功，{@code 0} 表示类别不存在或已删除
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
     * <p>使用 MySQL 8.0+ 递归 CTE 从起点的父节点向上遍历，仅访问未逻辑删除行；最多查询
     * 10 层以限制异常环或超深数据的影响，{@code DISTINCT} 去除重复祖先。查询不加锁，主要用于
     * 设置父类别前检测自身或后代回指形成的循环。
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
