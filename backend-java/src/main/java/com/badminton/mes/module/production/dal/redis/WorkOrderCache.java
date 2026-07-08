package com.badminton.mes.module.production.dal.redis;

import java.util.Optional;

import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import tools.jackson.databind.ObjectMapper;

/**
 * 工单详情 Redis 缓存。
 *
 * <p>缓存是弱依赖：读取、写入、删除失败只记录日志，不阻断主业务。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
@Component
public class WorkOrderCache {

    private static final Logger logger = LoggerFactory.getLogger(WorkOrderCache.class);

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    /**
     * 构造工单缓存组件。
     *
     * @param stringRedisTemplate Redis 操作模板
     * @param objectMapper        JSON 序列化器
     */
    public WorkOrderCache(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 读取工单详情缓存。
     *
     * @param id 工单主键
     * @return 缓存命中的工单实体
     */
    public Optional<WorkOrderEntity> get(Long id) {
        String key = ProductionRedisKeyConstants.workOrderDetailKey(id);
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(json)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, WorkOrderCacheDTO.class).toEntity());
        } catch (RuntimeException e) {
            logger.warn("[工单缓存读取失败] id: {}, errorMessage: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 写入工单详情缓存。
     *
     * @param entity 工单实体
     */
    public void put(WorkOrderEntity entity) {
        try {
            String json = objectMapper.writeValueAsString(WorkOrderCacheDTO.fromEntity(entity));
            stringRedisTemplate.opsForValue().set(
                    ProductionRedisKeyConstants.workOrderDetailKey(entity.getId()),
                    json, ProductionRedisKeyConstants.WORK_ORDER_DETAIL_TTL);
        } catch (RuntimeException e) {
            logger.warn("[工单缓存写入失败] id: {}, errorMessage: {}", entity.getId(), e.getMessage());
        }
    }

    /**
     * 删除工单详情缓存。
     *
     * @param id 工单主键
     */
    public void evict(Long id) {
        try {
            stringRedisTemplate.delete(ProductionRedisKeyConstants.workOrderDetailKey(id));
        } catch (RuntimeException e) {
            logger.error("[工单缓存删除失败] id: {}, errorMessage: {}", id, e.getMessage(), e);
        }
    }
}
