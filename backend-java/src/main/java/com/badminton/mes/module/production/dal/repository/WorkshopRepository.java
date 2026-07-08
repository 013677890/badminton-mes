package com.badminton.mes.module.production.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.production.dal.entity.WorkshopEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 车间 JPA Repository。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
public interface WorkshopRepository extends JpaRepository<WorkshopEntity, Long> {

    /**
     * 按主键查询未删除的车间。
     *
     * @param id 车间主键
     * @return 车间实体
     */
    Optional<WorkshopEntity> findByIdAndDeletedFalse(Long id);
}
