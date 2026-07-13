package com.badminton.mes.module.andon.dal.repository;

import java.util.List;

import com.badminton.mes.module.andon.dal.entity.AndonProcessLogEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/** 安灯异常处理过程 Repository。 */
public interface AndonProcessLogRepository extends JpaRepository<AndonProcessLogEntity, Long> {

    List<AndonProcessLogEntity> findByEventIdOrderByIdAsc(Long eventId);
}
