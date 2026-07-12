package com.badminton.mes.module.production.dal.repository;

import java.util.List;

import com.badminton.mes.module.production.dal.entity.WorkOrderMaterialEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 工单物料需求 JPA Repository。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
public interface WorkOrderMaterialRepository extends JpaRepository<WorkOrderMaterialEntity, Long> {

    /** 判断物料是否被任意工单物料需求引用。 */
    boolean existsByMaterialIdAndDeletedFalse(Long materialId);

    /** 判断物料是否被未结束工单物料需求引用。 */
    @Query("""
            SELECT CASE WHEN COUNT(material) > 0 THEN true ELSE false END
            FROM WorkOrderMaterialEntity material, WorkOrderEntity workOrder
            WHERE workOrder.id = material.workOrderId
              AND material.materialId = :materialId
              AND workOrder.orderStatus IN :statuses
              AND workOrder.deleted = false
              AND material.deleted = false
            """)
    boolean existsActiveOrderByMaterialId(
            @Param("materialId") Long materialId,
            @Param("statuses") java.util.Collection<Integer> statuses);

    /**
     * 查询指定工单的全部未删除物料需求。
     *
     * @param workOrderId 工单主键
     * @return 物料需求列表，无数据时为空集合
     */
    List<WorkOrderMaterialEntity> findByWorkOrderIdAndDeletedFalse(Long workOrderId);

    /**
     * 判断指定工单是否已生成物料需求，防止重复下达时重复生成。
     *
     * @param workOrderId 工单主键
     * @return true 已生成，false 未生成
     */
    boolean existsByWorkOrderIdAndDeletedFalse(Long workOrderId);

    /**
     * 逻辑删除指定工单的全部未删除物料需求，工单作废时随单失效，
     * 避免后续齐套/领料按物料聚合时计入已作废工单。
     *
     * @param workOrderId 工单主键
     * @return 影响行数
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE WorkOrderMaterialEntity material
            SET material.deleted = true,
                material.updateTime = CURRENT_TIMESTAMP
            WHERE material.workOrderId = :workOrderId
              AND material.deleted = false
            """)
    int logicDeleteByWorkOrderId(@Param("workOrderId") Long workOrderId);
}
