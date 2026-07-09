package com.badminton.mes.module.production.dal.repository;

import java.util.Collection;
import java.util.List;

import com.badminton.mes.module.production.dal.entity.MaterialEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 物料 JPA Repository，工单物料需求展示时批量回查物料档案。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
public interface MaterialRepository extends JpaRepository<MaterialEntity, Long> {

    /**
     * 按主键集合批量查询未删除物料，用于物料需求明细的名称/编码回填。
     *
     * @param ids 物料主键集合，调用方保证非空且规模有限(单工单 BOM 明细数)
     * @return 物料列表
     */
    List<MaterialEntity> findByIdInAndDeletedFalse(Collection<Long> ids);
}
