package com.badminton.mes.module.craft.dal.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.craft.dal.entity.CraftRouteProductEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 工艺路线产品关系 Repository。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public interface CraftRouteProductRepository extends JpaRepository<CraftRouteProductEntity, Long> {

    /**
     * 查询路线未删除产品关系。
     *
     * @param routeId 路线主键
     * @return 产品关系列表
     */
    List<CraftRouteProductEntity> findByRouteIdAndDeletedFalseOrderByProductIdAsc(Long routeId);

    /**
     * 查询产品当前默认路线关系。
     *
     * @param productId 产品主键
     * @return 默认路线关系
     */
    Optional<CraftRouteProductEntity> findByProductIdAndDefaultRouteTrueAndDeletedFalse(Long productId);

    /**
     * 判断路线是否仍绑定指定产品。
     *
     * @param routeId   路线主键
     * @param productId 产品主键
     * @return true 表示存在有效绑定
     */
    boolean existsByRouteIdAndProductIdAndDeletedFalse(Long routeId, Long productId);

    /** 判断产品是否被任意路线关系引用。 */
    boolean existsByProductIdAndDeletedFalse(Long productId);

    /** 判断产品是否被生效路线引用。 */
    @Query("""
            SELECT CASE WHEN COUNT(relation) > 0 THEN true ELSE false END
            FROM CraftRouteProductEntity relation, CraftRouteEntity route
            WHERE route.id = relation.routeId
              AND relation.productId = :productId
              AND route.routingStatus = :routeStatus
              AND route.deleted = false
              AND relation.deleted = false
            """)
    boolean existsEffectiveRouteByProductId(
            @Param("productId") Long productId,
            @Param("routeStatus") Integer routeStatus);

    /**
     * 逻辑删除路线全部产品关系。
     *
     * @param routeId    路线主键
     * @param operatorId 操作人主键
     * @return 影响行数
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE CraftRouteProductEntity relation
            SET relation.deleted = true,
                relation.defaultRoute = false,
                relation.updateBy = :operatorId,
                relation.version = relation.version + 1
            WHERE relation.routeId = :routeId
              AND relation.deleted = false
            """)
    int logicDeleteByRouteId(@Param("routeId") Long routeId,
                             @Param("operatorId") Long operatorId);

    /**
     * 清除指定产品原有默认路线。
     *
     * @param productIds 产品主键集合
     * @param routeId    当前待生效路线主键
     * @param operatorId 操作人主键
     * @return 影响行数
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE CraftRouteProductEntity relation
            SET relation.defaultRoute = false,
                relation.updateBy = :operatorId,
                relation.version = relation.version + 1
            WHERE relation.productId IN :productIds
              AND relation.routeId <> :routeId
              AND relation.defaultRoute = true
              AND relation.deleted = false
            """)
    int clearOtherDefaults(@Param("productIds") Collection<Long> productIds,
                           @Param("routeId") Long routeId,
                           @Param("operatorId") Long operatorId);

    /**
     * 将指定路线全部有效产品关系设为默认。
     *
     * @param routeId    路线主键
     * @param operatorId 操作人主键
     * @return 影响行数
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE CraftRouteProductEntity relation
            SET relation.defaultRoute = true,
                relation.updateBy = :operatorId,
                relation.version = relation.version + 1
            WHERE relation.routeId = :routeId
              AND relation.deleted = false
            """)
    int markRouteAsDefault(@Param("routeId") Long routeId,
                           @Param("operatorId") Long operatorId);

    /**
     * 清除指定路线的默认标记。
     *
     * @param routeId    路线主键
     * @param operatorId 操作人主键
     * @return 影响行数
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE CraftRouteProductEntity relation
            SET relation.defaultRoute = false,
                relation.updateBy = :operatorId,
                relation.version = relation.version + 1
            WHERE relation.routeId = :routeId
              AND relation.defaultRoute = true
              AND relation.deleted = false
            """)
    int clearRouteDefaults(@Param("routeId") Long routeId,
                           @Param("operatorId") Long operatorId);
}
