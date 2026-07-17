package com.badminton.mes.module.craft.service.impl;

import com.badminton.mes.module.craft.dal.repository.CraftRouteProductRepository;
import com.badminton.mes.module.craft.enums.CraftRouteStatusEnum;
import com.badminton.mes.module.craft.service.ProductRouteReferenceQuery;

import org.springframework.stereotype.Service;

/**
 * 产品路线反向引用查询实现。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@Service
public class ProductRouteReferenceQueryImpl implements ProductRouteReferenceQuery {

    private final CraftRouteProductRepository routeProductRepository;

    /**
     * 构造器注入。
     *
     * @param routeProductRepository 路线产品关系 Repository
     */
    public ProductRouteReferenceQueryImpl(CraftRouteProductRepository routeProductRepository) {
        this.routeProductRepository = routeProductRepository;
    }

    @Override
    public boolean hasEffectiveRoute(Long productId) {
        // 将状态条件下推到数据库 exists 查询，不加载完整路线关系和主表实体。
        return routeProductRepository.existsEffectiveRouteByProductId(
                productId, CraftRouteStatusEnum.EFFECTIVE.getStatus());
    }

    @Override
    public boolean hasAnyRouteBinding(Long productId) {
        return routeProductRepository.existsByProductIdAndDeletedFalse(productId);
    }
}
