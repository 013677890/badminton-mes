package com.badminton.mes.module.craft.service.dto;

import java.util.Map;

import com.badminton.mes.module.craft.dal.entity.CraftProcessEntity;
import com.badminton.mes.module.production.dal.entity.ProductEntity;

/**
 * 工艺路线引用校验结果。
 *
 * @param productMap 可用产品映射
 * @param processMap 可用工序映射
 * @author 张竹灏
 * @date 2026/07/10
 */
public record CraftRouteReferenceContext(
        Map<Long, ProductEntity> productMap,
        Map<Long, CraftProcessEntity> processMap) {
}
