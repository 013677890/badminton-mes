package com.badminton.mes.module.quality.dal.redis;

import java.time.Duration;

/**
 * 质量模块 Redis Key 与缓存资源命名约定。
 *
 * <p>每个详情 Key 都有同维度的版本 Key：详情值按固定 TTL 自动过期，版本 Key 则作为 Lua 并发控制标记
 * 持续保留。业务更新在事务提交后推进版本并删除详情；分类和项目等组合详情由服务层根据引用关系级联失效。
 */
public final class QualityRedisKeyConstants {

    /** 详情缓存 Key 模板，参数依次为资源类型与业务主键，值为序列化后的响应对象。 */
    public static final String QUALITY_DETAIL = "mes:quality:%s:%d";

    /** 详情版本 Key 模板，供 Lua 比较或递增，用于阻止事务更新期间的并发查询回填旧值。 */
    public static final String QUALITY_DETAIL_VERSION = "mes:quality:%s:%d:version";

    /** 详情缓存统一存活 30 分钟；到期后按 Cache-Aside 策略重新回源，版本 Key 不使用该 TTL。 */
    public static final Duration QUALITY_DETAIL_TTL = Duration.ofMinutes(30);

    /** 检验分类详情资源名；分类变化还需级联失效归属该分类的项目详情。 */
    public static final String INSPECTION_CATEGORY_RESOURCE = "inspection_category";

    /** 检验项目详情资源名；项目变化还需级联失效引用该项目的方案详情。 */
    public static final String INSPECTION_ITEM_RESOURCE = "inspection_item";

    /** 检验标准方案版本详情资源名，主键维度对应一个具体且可追溯的方案版本。 */
    public static final String INSPECTION_PLAN_RESOURCE = "inspection_plan";

    /** 检验单聚合详情资源名，结果录入和提交会在事务成功后失效该详情。 */
    public static final String INSPECTION_RECORD_RESOURCE = "inspection_record";

    /**
     * 构造质量模块详情缓存 Key。
     *
     * @param resourceName 资源类型
     * @param id           业务主键
     * @return 缓存 Key
     */
    public static String detailKey(String resourceName, Long id) {
        return String.format(QUALITY_DETAIL, resourceName, id);
    }

    /** 构造与详情 Key 同资源、同主键维度的版本 Key。 */
    public static String detailVersionKey(String resourceName, Long id) {
        return String.format(QUALITY_DETAIL_VERSION, resourceName, id);
    }

    private QualityRedisKeyConstants() {
    }
}
