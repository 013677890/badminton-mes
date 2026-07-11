package com.badminton.mes.module.craft.dal.repository;

import java.util.List;

import com.badminton.mes.module.craft.dal.entity.CraftRouteDetailEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 工艺路线明细 Repository。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public interface CraftRouteDetailRepository extends JpaRepository<CraftRouteDetailEntity, Long> {

    /**
     * 判断工序是否仍被有效路线明细引用。
     *
     * @param processId 工序主键
     * @return true 已引用
     */
    boolean existsByProcessIdAndDeletedFalse(Long processId);

    /**
     * 判断工序是否被生效路线引用。
     *
     * @param processId      工序主键
     * @param effectiveStatus 路线生效状态
     * @return true 表示存在生效路线引用
     */
    @Query("""
            SELECT CASE WHEN COUNT(detail.id) > 0 THEN true ELSE false END
            FROM CraftRouteDetailEntity detail, CraftRouteEntity route
            WHERE detail.routeId = route.id
              AND detail.processId = :processId
              AND detail.deleted = false
              AND route.routingStatus = :effectiveStatus
              AND route.deleted = false
            """)
    boolean existsEffectiveRouteByProcessId(
            @Param("processId") Long processId,
            @Param("effectiveStatus") Integer effectiveStatus);

    /**
     * 判断 SOP 是否被生效路线引用。
     *
     * @param sopId           SOP 主键
     * @param effectiveStatus 路线生效状态
     * @return true 表示存在生效路线引用
     */
    @Query("""
            SELECT CASE WHEN COUNT(detail.id) > 0 THEN true ELSE false END
            FROM CraftRouteDetailEntity detail, CraftRouteEntity route
            WHERE detail.routeId = route.id
              AND detail.sopId = :sopId
              AND detail.deleted = false
              AND route.routingStatus = :effectiveStatus
              AND route.deleted = false
            """)
    boolean existsEffectiveRouteBySopId(
            @Param("sopId") Long sopId,
            @Param("effectiveStatus") Integer effectiveStatus);

    /**
     * 判断设备类别是否被生效路线直接引用。
     *
     * @param categoryId      设备类别主键
     * @param effectiveStatus 路线生效状态
     * @return true 表示存在生效路线引用
     */
    @Query("""
            SELECT CASE WHEN COUNT(detail.id) > 0 THEN true ELSE false END
            FROM CraftRouteDetailEntity detail, CraftRouteEntity route
            WHERE detail.routeId = route.id
              AND detail.equipmentCategoryId = :categoryId
              AND detail.deleted = false
              AND route.routingStatus = :effectiveStatus
              AND route.deleted = false
            """)
    boolean existsEffectiveRouteByEquipmentCategoryId(
            @Param("categoryId") Long categoryId,
            @Param("effectiveStatus") Integer effectiveStatus);

    /**
     * 查询路线未删除明细并按顺序返回。
     *
     * @param routeId 路线主键
     * @return 路线明细列表
     */
    List<CraftRouteDetailEntity> findByRouteIdAndDeletedFalseOrderBySequenceNoAsc(Long routeId);

    /**
     * 逻辑删除路线全部有效明细。
     *
     * @param routeId    路线主键
     * @param operatorId 操作人主键
     * @return 影响行数
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE CraftRouteDetailEntity detail
            SET detail.deleted = true,
                detail.updateBy = :operatorId,
                detail.version = detail.version + 1
            WHERE detail.routeId = :routeId
              AND detail.deleted = false
            """)
    int logicDeleteByRouteId(@Param("routeId") Long routeId,
                             @Param("operatorId") Long operatorId);
}
