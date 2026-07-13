package com.badminton.mes.module.production.dal.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.production.dal.entity.MaterialStockEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 物料库存快照 JPA Repository，齐套分析批量读取可用量。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public interface MaterialStockRepository extends JpaRepository<MaterialStockEntity, Long> {

    /** 判断物料是否存在库存记录。 */
    boolean existsByMaterialIdAndDeletedFalse(Long materialId);

    Optional<MaterialStockEntity> findByMaterialIdAndDeletedFalse(Long materialId);

    /**
     * 按物料主键集合批量查询未删除库存快照，一次分析只查一趟避免 N+1。
     *
     * @param materialIds 物料主键集合，调用方保证非空(单工单 BOM 明细规模)
     * @return 库存快照列表，无记录的物料不在结果中(按可用 0 处理)
     */
    List<MaterialStockEntity> findByMaterialIdInAndDeletedFalse(Collection<Long> materialIds);

    /**
     * 按外部同步时间原子写入库存快照；旧快照不会覆盖新数据。
     *
     * @return 影响行数，0 表示快照过期或内容没有变化
     */
    @Modifying
    @Query(value = """
            INSERT INTO material_stock (
              material_id, source_system, available_quantity, locked_quantity,
              checking_quantity, transit_quantity, sync_time, create_time, update_time, is_deleted
            ) VALUES (
              :materialId, :sourceSystem, :availableQuantity, :lockedQuantity,
              :checkingQuantity, :transitQuantity, :syncTime, CURRENT_TIMESTAMP,
              CURRENT_TIMESTAMP, 0
            )
            ON DUPLICATE KEY UPDATE
              source_system = IF(VALUES(sync_time) >= sync_time,
                VALUES(source_system), source_system),
              available_quantity = IF(VALUES(sync_time) >= sync_time,
                VALUES(available_quantity), available_quantity),
              locked_quantity = IF(VALUES(sync_time) >= sync_time,
                VALUES(locked_quantity), locked_quantity),
              checking_quantity = IF(VALUES(sync_time) >= sync_time,
                VALUES(checking_quantity), checking_quantity),
              transit_quantity = IF(VALUES(sync_time) >= sync_time,
                VALUES(transit_quantity), transit_quantity),
              update_time = IF(VALUES(sync_time) >= sync_time,
                CURRENT_TIMESTAMP, update_time),
              is_deleted = IF(VALUES(sync_time) >= sync_time, 0, is_deleted),
              sync_time = GREATEST(sync_time, VALUES(sync_time))
            """, nativeQuery = true)
    int upsertIfNewer(@Param("materialId") Long materialId,
                      @Param("sourceSystem") String sourceSystem,
                      @Param("availableQuantity") java.math.BigDecimal availableQuantity,
                      @Param("lockedQuantity") java.math.BigDecimal lockedQuantity,
                      @Param("checkingQuantity") java.math.BigDecimal checkingQuantity,
                      @Param("transitQuantity") java.math.BigDecimal transitQuantity,
                      @Param("syncTime") java.time.LocalDateTime syncTime);
}
