package com.badminton.mes.module.craft.service.dto;

/**
 * 工艺路线步骤审计快照。
 *
 * @param sequenceNo         工序顺序
 * @param processId          工序主键
 * @param stationId          工位主键
 * @param equipmentCategoryId 设备类别主键
 * @param inspectNode        是否质检节点
 * @param sopId              SOP 关联主键
 * @param qualityPlanId      检验方案主键
 * @author 张竹灏
 * @date 2026/07/10
 */
public record CraftRouteStepSnapshotDTO(
        Integer sequenceNo,
        Long processId,
        Long stationId,
        Long equipmentCategoryId,
        Boolean inspectNode,
        Long sopId,
        Long qualityPlanId) {
}
