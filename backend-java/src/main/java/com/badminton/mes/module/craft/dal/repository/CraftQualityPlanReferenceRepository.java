package com.badminton.mes.module.craft.dal.repository;

import java.util.Collection;
import java.util.List;

import com.badminton.mes.module.craft.dal.entity.CraftQualityPlanReferenceEntity;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.Repository;

import jakarta.persistence.LockModeType;

/**
 * 工序对质量检验方案的只读引用 Repository。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public interface CraftQualityPlanReferenceRepository
        extends Repository<CraftQualityPlanReferenceEntity, Long> {

    /**
     * 判断检验方案是否存在、启用且未删除。
     *
     * @param id     检验方案主键
     * @param status 启用状态
     * @return true 表示方案可被工序引用
     */
    boolean existsByIdAndStatusAndDeletedFalse(Long id, Integer status);

    /**
     * 批量查询启用且未删除检验方案。
     *
     * @param ids    检验方案主键集合
     * @param status 启用状态
     * @return 可用检验方案列表
     */
    List<CraftQualityPlanReferenceEntity> findByIdInAndStatusAndDeletedFalse(
            Collection<Long> ids, Integer status);

    /**
     * 按主键升序写锁可用检验方案，供路线审核在锁内复核引用。
     *
     * @param ids    检验方案主键集合
     * @param status 启用状态
     * @return 已锁定检验方案列表
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT plan FROM CraftQualityPlanReferenceEntity plan
            WHERE plan.id IN :ids
              AND plan.status = :status
              AND plan.deleted = false
            ORDER BY plan.id ASC
            """)
    List<CraftQualityPlanReferenceEntity> findAvailableByIdInForUpdateOrderByIdAsc(
            @Param("ids") Collection<Long> ids, @Param("status") Integer status);
}
