package com.badminton.mes.module.craft.dal.redis;

import java.time.Duration;

/**
 * 工艺模块 Redis Key 常量，集中管理 Key 格式与 TTL。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
public final class CraftRedisKeyConstants {

    /** 产品默认工艺路线聚合缓存 Key 模板，参数为产品主键 id */
    public static final String DEFAULT_ROUTE = "mes:craft:route:default:%d";

    /** 产品默认工艺路线聚合缓存 TTL */
    public static final Duration DEFAULT_ROUTE_TTL = Duration.ofHours(1);

    /** 工序详情缓存 Key 模板，参数为工序主键 id */
    public static final String PROCESS_DETAIL = "mes:craft:process:%d";

    /** 工序 SOP 列表缓存 Key 模板，参数为工序主键 id */
    public static final String PROCESS_SOPS = "mes:craft:process:%d:sops";

    /** 工序不良原因列表缓存 Key 模板，参数为工序主键 id */
    public static final String PROCESS_DEFECT_REASONS = "mes:craft:process:%d:defect_reasons";

    /** 工序基础资料缓存 TTL */
    public static final Duration PROCESS_DATA_TTL = Duration.ofMinutes(45);

    /**
     * 构造产品默认工艺路线缓存 Key。
     *
     * @param productId 产品主键
     * @return 缓存 Key
     */
    public static String defaultRouteKey(Long productId) {
        return String.format(DEFAULT_ROUTE, productId);
    }

    /**
     * 构造工序详情缓存 Key。
     *
     * @param processId 工序主键
     * @return 缓存 Key
     */
    public static String processDetailKey(Long processId) {
        return String.format(PROCESS_DETAIL, processId);
    }

    /**
     * 构造工序 SOP 列表缓存 Key。
     *
     * @param processId 工序主键
     * @return 缓存 Key
     */
    public static String processSopsKey(Long processId) {
        return String.format(PROCESS_SOPS, processId);
    }

    /**
     * 构造工序不良原因列表缓存 Key。
     *
     * @param processId 工序主键
     * @return 缓存 Key
     */
    public static String processDefectReasonsKey(Long processId) {
        return String.format(PROCESS_DEFECT_REASONS, processId);
    }

    private CraftRedisKeyConstants() {
    }
}
