package com.badminton.mes.module.craft.service.dto;

import java.util.List;

import com.badminton.mes.module.craft.dal.entity.CraftRouteDetailEntity;
import com.badminton.mes.module.craft.dal.entity.CraftRouteProductEntity;

/**
 * 工艺路线聚合子记录。
 *
 * @param products 产品关系
 * @param details  路线明细
 * @author 张竹灏
 * @date 2026/07/10
 */
public record CraftRouteChildren(
        List<CraftRouteProductEntity> products,
        List<CraftRouteDetailEntity> details) {
}
