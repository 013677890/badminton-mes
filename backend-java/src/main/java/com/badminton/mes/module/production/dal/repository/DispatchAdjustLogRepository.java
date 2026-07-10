package com.badminton.mes.module.production.dal.repository;

import java.util.List;

import com.badminton.mes.module.production.dal.entity.DispatchAdjustLogEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 派工单排产调整日志 JPA Repository。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public interface DispatchAdjustLogRepository extends JpaRepository<DispatchAdjustLogEntity, Long> {

    /**
     * 查询派工单的调整日志，最新在前。
     *
     * @param dispatchOrderId 派工单主键
     * @return 调整日志列表，无数据时为空集合
     */
    List<DispatchAdjustLogEntity> findByDispatchOrderIdAndDeletedFalseOrderByIdDesc(Long dispatchOrderId);
}
