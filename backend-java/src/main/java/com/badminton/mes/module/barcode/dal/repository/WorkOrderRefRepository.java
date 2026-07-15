package com.badminton.mes.module.barcode.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.barcode.dal.entity.WorkOrderRefEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 生产工单只读引用 Repository，B 组按协作边界直接只读查询 A 组 prod_work_order。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public interface WorkOrderRefRepository extends JpaRepository<WorkOrderRefEntity, Long> {

    /**
     * 按主键查询未删除的生产工单。
     *
     * @param id 工单主键
     * @return 工单只读引用
     */
    Optional<WorkOrderRefEntity> findByIdAndDeletedFalse(Long id);
}
