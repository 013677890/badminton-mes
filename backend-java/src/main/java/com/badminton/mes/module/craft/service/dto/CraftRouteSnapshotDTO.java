package com.badminton.mes.module.craft.service.dto;

import java.util.List;

/**
 * 工艺路线聚合审计快照。
 *
 * @param routingCode      路线编码
 * @param routingName      路线名称
 * @param routingVersion   业务版本
 * @param previousRouteId  上一版本路线主键
 * @param sourceType       来源
 * @param routingStatus    状态
 * @param version          乐观锁版本
 * @param productIds       适用产品主键
 * @param steps            路线步骤
 * @author 张竹灏
 * @date 2026/07/10
 */
public record CraftRouteSnapshotDTO(
        String routingCode,
        String routingName,
        String routingVersion,
        Long previousRouteId,
        Integer sourceType,
        Integer routingStatus,
        Integer version,
        List<Long> productIds,
        List<CraftRouteStepSnapshotDTO> steps) {
}
