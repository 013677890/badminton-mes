package com.badminton.mes.module.andon.dal.repository;

import java.util.List;

import com.badminton.mes.module.andon.dal.entity.AndonNotificationRecordEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/** 安灯异常通知记录 Repository。 */
public interface AndonNotificationRecordRepository
        extends JpaRepository<AndonNotificationRecordEntity, Long> {

    List<AndonNotificationRecordEntity> findByEventIdOrderByIdAsc(Long eventId);
}
