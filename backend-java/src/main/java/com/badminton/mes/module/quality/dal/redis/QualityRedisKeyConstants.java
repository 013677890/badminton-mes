package com.badminton.mes.module.quality.dal.redis;

import java.time.Duration;

/**
 * 质量模块 Redis Key 常量，集中管理详情缓存 Key 格式与 TTL。
 */
public final class QualityRedisKeyConstants {

    /** 质量模块详情缓存 Key 模板，参数为资源类型与业务主键 id。 */
    public static final String QUALITY_DETAIL = "mes:quality:%s:%d";

    /** 质量模块详情缓存版本 Key 模板，用于阻止并发查询回填旧值。 */
    public static final String QUALITY_DETAIL_VERSION = "mes:quality:%s:%d:version";

    /** 质量模块详情缓存 TTL。 */
    public static final Duration QUALITY_DETAIL_TTL = Duration.ofMinutes(30);

    /** 检验分类详情缓存资源名。 */
    public static final String INSPECTION_CATEGORY_RESOURCE = "inspection_category";

    /** 检验项目详情缓存资源名。 */
    public static final String INSPECTION_ITEM_RESOURCE = "inspection_item";

    /** 检验标准方案详情缓存资源名。 */
    public static final String INSPECTION_PLAN_RESOURCE = "inspection_plan";

    /** 检验单详情缓存资源名。 */
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

    /** 构造质量模块详情缓存版本 Key。 */
    public static String detailVersionKey(String resourceName, Long id) {
        return String.format(QUALITY_DETAIL_VERSION, resourceName, id);
    }

    private QualityRedisKeyConstants() {
    }
}
