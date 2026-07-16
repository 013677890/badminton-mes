package com.badminton.mes.module.andon.dal.repository;

import java.util.List;

import com.badminton.mes.module.andon.dal.entity.AndonProcessLogEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 安灯事件聚合中的状态迁移与责任主体变更日志数据访问接口。
 *
 * <p>过程日志随事件动作追加，不独立修改；按事件和主键升序读取用于重建确认、处理、转派、完成、
 * 升级及关闭的审计时间线。
 */
public interface AndonProcessLogRepository extends JpaRepository<AndonProcessLogEntity, Long> {

    /**
     * 按主键升序查询事件的全部处理日志，以稳定顺序还原状态迁移和责任主体变化轨迹。
     */
    List<AndonProcessLogEntity> findByEventIdOrderByIdAsc(Long eventId);
}
