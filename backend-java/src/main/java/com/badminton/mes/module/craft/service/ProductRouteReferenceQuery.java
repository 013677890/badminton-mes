package com.badminton.mes.module.craft.service;

/**
 * 工艺模块提供的产品路线反向引用查询契约。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
public interface ProductRouteReferenceQuery {

    /**
     * 判断产品是否被生效工艺路线绑定。
     *
     * @param productId 产品主键
     * @return true 表示存在生效路线绑定
     */
    boolean hasEffectiveRoute(Long productId);

    /**
     * 判断产品是否存在任意未删除路线绑定。
     *
     * @param productId 产品主键
     * @return true 表示存在路线绑定
     */
    boolean hasAnyRouteBinding(Long productId);
}
