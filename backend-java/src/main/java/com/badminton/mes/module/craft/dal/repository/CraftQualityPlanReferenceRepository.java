package com.badminton.mes.module.craft.dal.repository;

import com.badminton.mes.module.craft.dal.entity.CraftQualityPlanReferenceEntity;

import org.springframework.data.repository.Repository;

/**
 * 工序对质量检验方案的只读引用 Repository。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public interface CraftQualityPlanReferenceRepository
        extends Repository<CraftQualityPlanReferenceEntity, Long> {

    /**
     * 判断检验方案是否存在、启用且未删除。
     *
     * @param id     检验方案主键
     * @param status 启用状态
     * @return true 表示方案可被工序引用
     */
    boolean existsByIdAndStatusAndDeletedFalse(Long id, Integer status);
}
