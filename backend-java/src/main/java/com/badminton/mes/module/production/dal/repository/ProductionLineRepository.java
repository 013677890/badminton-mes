package com.badminton.mes.module.production.dal.repository;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.production.dal.entity.ProductionLineEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 产线 JPA Repository，派工时校验产线可用性与产能，只读。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public interface ProductionLineRepository extends JpaRepository<ProductionLineEntity, Long> {

    /**
     * 按主键查询未删除产线。
     *
     * @param id 产线主键
     * @return 产线实体
     */
    Optional<ProductionLineEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 查询指定车间的启用产线，排产建议候选。
     *
     * @param workshopId 车间主键
     * @param status     状态(传启用)
     * @return 产线列表，无数据时为空集合
     */
    List<ProductionLineEntity> findByWorkshopIdAndStatusAndDeletedFalseOrderByIdAsc(
            Long workshopId, Integer status);
}
