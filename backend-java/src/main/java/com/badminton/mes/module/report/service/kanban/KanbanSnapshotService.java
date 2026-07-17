package com.badminton.mes.module.report.service.kanban;

import java.util.Map;
import java.util.Set;

/**
 * 看板快照服务。
 *
 * <p>看板 Controller 读取快照，刷新任务或 WebSocket 推送服务调用 {@link #refresh(String, Long)}
 * 重新聚合实时数据；Redis 只保存可重建的快照，不作为生产事实数据源。
 *
 * @author 刘涵
 * @date 2026/07/16
 */
public interface KanbanSnapshotService {
    /** 读取指定范围的看板快照，未命中时由实现类决定是否回源计算。 */
    Map<String, Object> get(String scopeType, Long scopeId);
    /** 重新聚合并缓存指定范围的最新看板快照。 */
    Map<String, Object> refresh(String scopeType, Long scopeId);
    /** 返回当前系统已登记、可被推送刷新的看板范围。 */
    Set<Scope> knownScopes();
    /** 看板范围：例如全厂、车间或产线。 */
    record Scope(String type, Long id) { }
}
