package com.badminton.mes.module.report.service.impl.kanban;

import com.badminton.mes.module.report.controller.vo.RealtimeReportQueryReqVO;
import com.badminton.mes.module.report.dal.redis.KanbanRedisKeyConstants;
import com.badminton.mes.module.report.service.kanban.KanbanSnapshotService;
import com.badminton.mes.module.report.service.RealtimeProductionService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** 看板快照 Redis 缓存实现。 @author 刘涵 */
@Service
public class KanbanSnapshotServiceImpl implements KanbanSnapshotService {
    private final RealtimeProductionService realtimeService;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Set<Scope> knownScopes = ConcurrentHashMap.newKeySet();

    public KanbanSnapshotServiceImpl(RealtimeProductionService realtimeService, StringRedisTemplate redis,
                                     ObjectMapper objectMapper) {
        this.realtimeService = realtimeService; this.redis = redis; this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> get(String scopeType, Long scopeId) {
        knownScopes.add(new Scope(scopeType, scopeId));
        String key = KanbanRedisKeyConstants.snapshotKey(scopeType, scopeId);
        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<Map<String, Object>>() { });
            } catch (Exception ignored) {
                // 缓存损坏时回源重建，MySQL 仍是事实数据源。
            }
        }
        return refresh(scopeType, scopeId);
    }

    @Override
    public Map<String, Object> refresh(String scopeType, Long scopeId) {
        knownScopes.add(new Scope(scopeType, scopeId));
        String key = KanbanRedisKeyConstants.snapshotKey(scopeType, scopeId);
        var request = new RealtimeReportQueryReqVO();
        if ("line".equals(scopeType)) request.setLineId(scopeId);
        if ("workshop".equals(scopeType)) request.setWorkshopId(scopeId);
        var overview = realtimeService.overviewForKanban(request);
        var result = new LinkedHashMap<String, Object>();
        result.put("snapshotTime", Instant.now()); result.put("lastRefreshTime", Instant.now());
        result.put("dataStatus", "FRESH"); result.put("sourceWarnings", java.util.List.of());
        result.put("version", Instant.now().toEpochMilli()); result.put("overview", overview);
        try { redis.opsForValue().set(key, objectMapper.writeValueAsString(result), KanbanRedisKeyConstants.SNAPSHOT_TTL); }
        catch (Exception ignored) { result.put("dataStatus", "PARTIAL"); result.put("sourceWarnings", java.util.List.of("Redis 快照写入失败")); }
        return result;
    }

    @Override
    public Set<Scope> knownScopes() { return Set.copyOf(knownScopes); }
}
