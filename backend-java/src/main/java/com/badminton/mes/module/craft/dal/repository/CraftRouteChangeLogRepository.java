package com.badminton.mes.module.craft.dal.repository;

import com.badminton.mes.module.craft.dal.entity.CraftRouteChangeLogEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 工艺路线变更日志 Repository。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public interface CraftRouteChangeLogRepository extends JpaRepository<CraftRouteChangeLogEntity, Long> {

    /**
     * 统计路线未删除日志数量。
     *
     * @param routeId 路线主键
     * @return 日志数量
     */
    long countByRouteIdAndDeletedFalse(Long routeId);

    /**
     * 分页查询路线未删除日志。
     *
     * @param routeId  路线主键
     * @param pageable 分页排序
     * @return 日志分页结果
     */
    Page<CraftRouteChangeLogEntity> findByRouteIdAndDeletedFalse(Long routeId, Pageable pageable);
}
