package com.badminton.mes.module.production.dal.repository;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.production.dal.entity.WorkOrderStatusLogEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 工单状态日志 JPA Repository。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
public interface WorkOrderStatusLogRepository extends JpaRepository<WorkOrderStatusLogEntity, Long> {

    /**
     * 按工单查询状态日志，最新在前。
     *
     * @param workOrderId 工单主键
     * @return 日志列表，无数据时为空集合
     */
    List<WorkOrderStatusLogEntity> findByWorkOrderIdAndDeletedFalseOrderByIdDesc(Long workOrderId);

    /**
     * 查询工单最近一条流转到指定状态的日志，恢复时用于还原暂停前状态。
     *
     * @param workOrderId 工单主键
     * @param toStatus    目标状态值
     * @return 最近一条日志
     */
    Optional<WorkOrderStatusLogEntity> findFirstByWorkOrderIdAndToStatusAndDeletedFalseOrderByIdDesc(
            Long workOrderId, Integer toStatus);
}
