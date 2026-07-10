package com.badminton.mes.module.craft.dal.repository;

import com.badminton.mes.module.craft.dal.entity.CraftRouteDetailEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 工艺路线明细 Repository。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public interface CraftRouteDetailRepository extends JpaRepository<CraftRouteDetailEntity, Long> {

    /**
     * 判断工序是否仍被有效路线明细引用。
     *
     * @param processId 工序主键
     * @return true 已引用
     */
    boolean existsByProcessIdAndDeletedFalse(Long processId);
}
