package com.badminton.mes.module.craft.dal.repository;

import java.util.Collection;
import java.util.List;

import com.badminton.mes.module.craft.dal.entity.CraftWorkstationReferenceEntity;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.Repository;

import jakarta.persistence.LockModeType;

/**
 * 工艺路线对工位的只读引用 Repository。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public interface CraftWorkstationReferenceRepository
        extends Repository<CraftWorkstationReferenceEntity, Long> {

    /**
     * 批量查询启用且未删除工位。
     *
     * @param ids    工位主键集合
     * @param status 启用状态
     * @return 可用工位列表
     */
    List<CraftWorkstationReferenceEntity> findByIdInAndStatusAndDeletedFalse(
            Collection<Long> ids, Integer status);

    /**
     * 按主键升序写锁可用工位，供路线审核在锁内复核引用。
     *
     * @param ids    工位主键集合
     * @param status 启用状态
     * @return 已锁定工位列表
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT station FROM CraftWorkstationReferenceEntity station
            WHERE station.id IN :ids
              AND station.status = :status
              AND station.deleted = false
            ORDER BY station.id ASC
            """)
    List<CraftWorkstationReferenceEntity> findAvailableByIdInForUpdateOrderByIdAsc(
            @Param("ids") Collection<Long> ids, @Param("status") Integer status);
}
