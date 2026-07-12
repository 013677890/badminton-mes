package com.badminton.mes.module.production.dal.repository;

import java.util.Collection;
import java.util.List;

import com.badminton.mes.module.production.dal.entity.MaterialStockEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 物料库存快照 JPA Repository，齐套分析批量读取可用量。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public interface MaterialStockRepository extends JpaRepository<MaterialStockEntity, Long> {

    /** 判断物料是否存在库存记录。 */
    boolean existsByMaterialIdAndDeletedFalse(Long materialId);

    /**
     * 按物料主键集合批量查询未删除库存快照，一次分析只查一趟避免 N+1。
     *
     * @param materialIds 物料主键集合，调用方保证非空(单工单 BOM 明细规模)
     * @return 库存快照列表，无记录的物料不在结果中(按可用 0 处理)
     */
    List<MaterialStockEntity> findByMaterialIdInAndDeletedFalse(Collection<Long> materialIds);
}
