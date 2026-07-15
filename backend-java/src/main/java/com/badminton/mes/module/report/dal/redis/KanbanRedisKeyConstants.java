package com.badminton.mes.module.report.dal.redis;

import java.time.Duration;

/** 看板 Redis Key 与过期时间。 @author 刘涵 */
public final class KanbanRedisKeyConstants {
    public static final Duration SNAPSHOT_TTL = Duration.ofSeconds(90);
    public static String snapshotKey(String scopeType, Long scopeId) {
        return "report:kanban:snapshot:" + scopeType + ":" + (scopeId == null ? "all" : scopeId);
    }
    private KanbanRedisKeyConstants() { }
}
