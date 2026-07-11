package com.badminton.mes.module.craft.dal.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.craft.dal.entity.CraftProcessSopEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 工序 SOP JPA Repository。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public interface CraftProcessSopRepository extends JpaRepository<CraftProcessSopEntity, Long> {

    /**
     * 查询工序的 SOP 列表。
     *
     * @param processId 工序主键
     * @return SOP 列表
     */
    List<CraftProcessSopEntity> findByProcessIdAndDeletedFalseOrderByIdAsc(Long processId);

    /**
     * 查询工序下指定 SOP。
     *
     * @param id        SOP 主键
     * @param processId 工序主键
     * @return SOP 实体
     */
    Optional<CraftProcessSopEntity> findByIdAndProcessIdAndDeletedFalse(Long id, Long processId);

    /**
     * 以写锁查询工序下未删除 SOP，和路线审核的引用锁串行化。
     *
     * @param id        SOP 主键
     * @param processId 工序主键
     * @return SOP 实体
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT sop FROM CraftProcessSopEntity sop
            WHERE sop.id = :id
              AND sop.processId = :processId
              AND sop.deleted = false
            """)
    Optional<CraftProcessSopEntity> findByIdAndProcessIdAndDeletedFalseForUpdate(
            @Param("id") Long id, @Param("processId") Long processId);

    /**
     * 判断同工序 SOP 编码是否重复。
     *
     * @param processId 工序主键
     * @param sopCode   SOP 编码
     * @return true 重复
     */
    boolean existsByProcessIdAndSopCodeAndDeletedFalse(Long processId, String sopCode);

    /**
     * 判断同工序 SOP 编码是否重复，排除指定记录。
     *
     * @param processId 工序主键
     * @param sopCode   SOP 编码
     * @param id        排除的 SOP 主键
     * @return true 重复
     */
    boolean existsByProcessIdAndSopCodeAndIdNotAndDeletedFalse(Long processId, String sopCode, Long id);

    /**
     * 判断工序是否仍存在未删除 SOP。
     *
     * @param processId 工序主键
     * @return true 表示存在未删除 SOP
     */
    boolean existsByProcessIdAndDeletedFalse(Long processId);

    /**
     * 批量查询启用且未删除 SOP。
     *
     * @param ids    SOP 关联主键集合
     * @param status 启用状态
     * @return 可用 SOP 列表
     */
    List<CraftProcessSopEntity> findByIdInAndStatusAndDeletedFalse(
            Collection<Long> ids, Integer status);

    /**
     * 按主键升序写锁可用 SOP，供路线审核在锁内复核引用。
     *
     * @param ids    SOP 主键集合
     * @param status 启用状态
     * @return 已锁定 SOP 列表
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT sop FROM CraftProcessSopEntity sop
            WHERE sop.id IN :ids
              AND sop.status = :status
              AND sop.deleted = false
            ORDER BY sop.id ASC
            """)
    List<CraftProcessSopEntity> findAvailableByIdInForUpdateOrderByIdAsc(
            @Param("ids") Collection<Long> ids, @Param("status") Integer status);
}
