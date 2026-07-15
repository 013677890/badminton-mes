package com.badminton.mes.module.scene.dal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * M2 对 A 组工单、基础资料和工艺契约的只读快照查询。
 *
 * <p>工艺事实统一读取 A 组业务代码实际写入的 craft_route / craft_route_detail /
 * craft_process / craft_process_sop 表，避免读取无人写入的契约占位表。
 *
 * @author 刘涵
 */
@Repository
public class SceneDependencyQueryRepository {
    private final JdbcTemplate jdbcTemplate;

    public SceneDependencyQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询处于已下达状态的工单快照，供 B 组自建任务与派工链路校验。
     */
    public Optional<WorkOrderSnapshot> findReleasedWorkOrder(Long workOrderId, Long lineId) {
        return findWorkOrderSnapshot(workOrderId, lineId, true);
    }

    /**
     * 查询工单快照供 A 组派工下发桥接使用：派工单业务状态已由生产模块校验，
     * 此处不再限定工单下达状态，只要求工单、产品、车间产线和工艺路线可用。
     */
    public Optional<WorkOrderSnapshot> findWorkOrderSnapshot(Long workOrderId, Long lineId) {
        return findWorkOrderSnapshot(workOrderId, lineId, false);
    }

    private Optional<WorkOrderSnapshot> findWorkOrderSnapshot(
            Long workOrderId, Long lineId, boolean requireReleased) {
        String workOrderSql = """
                SELECT t1.id, t1.work_order_no, t1.product_id, t2.product_code, t2.product_name,
                       t1.batch_no, t1.routing_id, t1.workshop_id, t1.plan_quantity, t1.dispatched_quantity
                FROM prod_work_order AS t1
                JOIN base_product AS t2 ON t2.id = t1.product_id AND t2.status = 1 AND t2.is_deleted = 0
                WHERE t1.id = ? AND t1.is_deleted = 0
                """ + (requireReleased ? " AND t1.order_status = 1" : "");
        List<WorkOrderCore> workOrders = jdbcTemplate.query(workOrderSql,
                (resultSet, rowNum) -> new WorkOrderCore(
                resultSet.getLong(1), resultSet.getString(2), resultSet.getLong(3),
                resultSet.getString(4), resultSet.getString(5), resultSet.getString(6),
                resultSet.getLong(7), resultSet.getLong(8), resultSet.getInt(9),
                resultSet.getInt(10)), workOrderId);
        if (workOrders.isEmpty()) {
            return Optional.empty();
        }
        WorkOrderCore core = workOrders.getFirst();
        List<ScopeSnapshot> scopes = jdbcTemplate.query("""
                SELECT t1.id, t1.workshop_name, t2.id, t2.line_name
                FROM base_workshop AS t1
                JOIN base_production_line AS t2 ON t2.workshop_id = t1.id
                WHERE t1.id = ? AND t2.id = ? AND t1.status = 1 AND t2.status = 1
                  AND t1.is_deleted = 0 AND t2.is_deleted = 0
                """, (resultSet, rowNum) -> new ScopeSnapshot(
                resultSet.getLong(1), resultSet.getString(2), resultSet.getLong(3),
                resultSet.getString(4)), core.workshopId(), lineId);
        List<RoutingSnapshot> routings = jdbcTemplate.query("""
                SELECT id, routing_code, routing_version FROM craft_route
                WHERE id = ? AND routing_status = 1 AND is_deleted = 0
                """, (resultSet, rowNum) -> new RoutingSnapshot(
                resultSet.getLong(1), resultSet.getString(2), resultSet.getString(3)), core.routingId());
        if (scopes.isEmpty() || routings.isEmpty()) {
            return Optional.empty();
        }
        ScopeSnapshot scope = scopes.getFirst();
        RoutingSnapshot routing = routings.getFirst();
        return Optional.of(new WorkOrderSnapshot(core.id(), core.workOrderNo(), core.productId(),
                core.productCode(), core.productName(), core.batchNo(), routing.id(), routing.code(),
                routing.version(), scope.workshopId(), scope.workshopName(), scope.lineId(),
                scope.lineName(), core.planQuantity(), core.dispatchedQuantity()));
    }

    public List<RoutingOperationSnapshot> findRoutingOperations(Long routingId) {
        return jdbcTemplate.query("""
                SELECT t1.process_id, t2.process_code, t2.process_name, t1.sequence_no,
                       t2.is_key_process, IF(t1.is_inspect = 1 OR t2.is_quality_required = 1, 1, 0),
                       t2.is_scan_required,
                       t1.sop_id, t3.sop_code, t3.sop_name, t3.sop_version, t1.station_id
                FROM craft_route_detail AS t1
                JOIN craft_process AS t2 ON t2.id = t1.process_id AND t2.status = 1 AND t2.is_deleted = 0
                LEFT JOIN craft_process_sop AS t3 ON t3.id = t1.sop_id AND t3.status = 1 AND t3.is_deleted = 0
                WHERE t1.route_id = ? AND t1.is_deleted = 0
                  AND (t1.sop_id IS NULL OR t3.id IS NOT NULL)
                ORDER BY t1.sequence_no ASC
                """, (resultSet, rowNum) -> new RoutingOperationSnapshot(
                resultSet.getLong(1), resultSet.getString(2), resultSet.getString(3),
                resultSet.getInt(4), resultSet.getBoolean(5), resultSet.getBoolean(6),
                resultSet.getBoolean(7), nullableLong(resultSet, 8), resultSet.getString(9),
                resultSet.getString(10), resultSet.getString(11), nullableLong(resultSet, 12)), routingId);
    }

    private static Long nullableLong(java.sql.ResultSet resultSet, int index) throws java.sql.SQLException {
        long value = resultSet.getLong(index);
        return resultSet.wasNull() ? null : value;
    }

    public record WorkOrderSnapshot(Long id, String workOrderNo, Long productId, String productCode,
                                    String productName, String batchNo, Long routingId, String routingCode,
                                    String routingVersion, Long workshopId, String workshopName, Long lineId,
                                    String lineName, Integer planQuantity, Integer dispatchedQuantity) {
    }

    private record WorkOrderCore(Long id, String workOrderNo, Long productId, String productCode,
                                 String productName, String batchNo, Long routingId, Long workshopId,
                                 Integer planQuantity, Integer dispatchedQuantity) {
    }

    private record ScopeSnapshot(Long workshopId, String workshopName, Long lineId, String lineName) {
    }

    private record RoutingSnapshot(Long id, String code, String version) {
    }

    public record RoutingOperationSnapshot(Long processId, String processCode, String processName, Integer sequence,
                                           boolean keyProcess, boolean inspect, boolean scanRequired, Long sopId,
                                           String sopCode, String sopName, String sopVersion, Long stationId) {
    }
}
