package com.badminton.mes.module.report.service.kanban;

import java.util.Map;
import java.util.Set;

/** 看板快照服务。 @author 刘涵 */
public interface KanbanSnapshotService {
    Map<String, Object> get(String scopeType, Long scopeId);
    Map<String, Object> refresh(String scopeType, Long scopeId);
    Set<Scope> knownScopes();
    record Scope(String type, Long id) { }
}
