package com.badminton.mes.module.production.dal.repository;

import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 工艺路线关系只读查询 Repository。
 *
 * <p>读取工艺模块业务代码实际写入的 craft_route / craft_route_product / craft_route_detail /
 * craft_process / craft_process_sop 表，仅按共同数据库契约读取下达工单所需事实，
 * 不引入工艺模块 Entity，也不对工艺表执行写操作。
 *
 * @author 刘涵
 */
@Repository
public class CraftRoutingRelationRepository {

    private static final String ROUTING_RELATION_SQL = """
            SELECT
                EXISTS(
                    SELECT t1.id
                    FROM craft_route AS t1
                    WHERE t1.id = :routingId
                      AND t1.routing_status = 1
                      AND t1.is_deleted = 0
                ) AS routing_available,
                EXISTS(
                    SELECT t2.id
                    FROM craft_route_product AS t2
                    WHERE t2.route_id = :routingId
                      AND t2.product_id = :productId
                      AND t2.is_deleted = 0
                ) AS product_bound,
                (
                    SELECT COUNT(*)
                    FROM craft_route_detail AS t3
                    WHERE t3.route_id = :routingId
                      AND t3.is_deleted = 0
                ) AS detail_count,
                (
                    SELECT MIN(t4.sequence_no)
                    FROM craft_route_detail AS t4
                    WHERE t4.route_id = :routingId
                      AND t4.is_deleted = 0
                ) AS minimum_sequence,
                (
                    SELECT MAX(t5.sequence_no)
                    FROM craft_route_detail AS t5
                    WHERE t5.route_id = :routingId
                      AND t5.is_deleted = 0
                ) AS maximum_sequence,
                (
                    SELECT COUNT(*)
                    FROM craft_route_detail AS t6
                    LEFT JOIN craft_process AS t7
                      ON t7.id = t6.process_id
                     AND t7.status = 1
                     AND t7.is_deleted = 0
                    WHERE t6.route_id = :routingId
                      AND t6.is_deleted = 0
                      AND t7.id IS NULL
                ) AS unavailable_process_count,
                (
                    SELECT COUNT(*)
                    FROM craft_route_detail AS t8
                    LEFT JOIN craft_process_sop AS t9
                      ON t9.id = t8.sop_id
                     AND t9.status = 1
                     AND t9.is_deleted = 0
                    WHERE t8.route_id = :routingId
                      AND t8.is_deleted = 0
                      AND t8.sop_id IS NOT NULL
                      AND t9.id IS NULL
                ) AS unavailable_sop_count
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CraftRoutingRelationRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询工艺路线下达校验所需的关系快照。
     *
     * @param routingId 工艺路线主键
     * @param productId 产品主键
     * @return 路线、产品、工序和 SOP 关系快照
     */
    public RoutingRelationSnapshot findRelationSnapshot(Long routingId, Long productId) {
        Map<String, Object> parameters = Map.of("routingId", routingId, "productId", productId);
        return jdbcTemplate.queryForObject(ROUTING_RELATION_SQL, parameters, (resultSet, rowNumber) ->
                new RoutingRelationSnapshot(
                        resultSet.getBoolean("routing_available"),
                        resultSet.getBoolean("product_bound"),
                        resultSet.getInt("detail_count"),
                        resultSet.getObject("minimum_sequence", Integer.class),
                        resultSet.getObject("maximum_sequence", Integer.class),
                        resultSet.getInt("unavailable_process_count"),
                        resultSet.getInt("unavailable_sop_count")));
    }

    /**
     * 工艺路线关系校验快照。
     *
     * @param routingAvailable        路线是否存在且生效
     * @param productBound            路线是否绑定目标产品
     * @param detailCount             有效路线明细数量
     * @param minimumSequence         最小工序顺序
     * @param maximumSequence         最大工序顺序
     * @param unavailableProcessCount 不可用工序数量
     * @param unavailableSopCount     不可用 SOP 数量
     */
    public record RoutingRelationSnapshot(
            boolean routingAvailable,
            boolean productBound,
            int detailCount,
            Integer minimumSequence,
            Integer maximumSequence,
            int unavailableProcessCount,
            int unavailableSopCount) {
    }
}
