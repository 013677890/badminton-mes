package com.badminton.mes.module.andon.dal.repository;

import java.util.List;

import com.badminton.mes.module.andon.dal.entity.AndonNotificationRecordEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 安灯事件聚合中的通知投递记录数据访问接口。
 *
 * <p>通知记录由事件流程统一创建，不作为独立主数据维护；按事件和主键顺序读取可还原各流程节点、
 * 通知渠道及模拟发送结果的完整轨迹。
 */
public interface AndonNotificationRecordRepository
        extends JpaRepository<AndonNotificationRecordEntity, Long> {

    /**
     * 按主键升序查询事件的全部通知记录，以稳定顺序展示各流程节点的投递渠道和发送结果。
     */
    List<AndonNotificationRecordEntity> findByEventIdOrderByIdAsc(Long eventId);
}
