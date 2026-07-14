package com.badminton.mes.module.craft.dal.repository;

import com.badminton.mes.module.craft.dal.entity.CraftProcessChangeLogEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 工序变更日志 JPA Repository。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public interface CraftProcessChangeLogRepository extends JpaRepository<CraftProcessChangeLogEntity, Long> {

    /**
     * 统计工序未删除的变更日志数量。
     *
     * @param processId 工序主键
     * @return 日志数量
     */
    long countByProcessIdAndDeletedFalse(Long processId);

    /**
     * 查询工序的变更日志，最新记录在前。
     *
     * @param processId 工序主键
     * @param pageable  分页及排序参数
     * @return 变更日志分页结果
     */
    Page<CraftProcessChangeLogEntity> findByProcessIdAndDeletedFalse(
            Long processId, Pageable pageable);
}
