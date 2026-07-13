package com.badminton.mes.module.craft.service.dto;

/**
 * 工序业务快照，用于变更日志 JSON 序列化。
 *
 * @param processCode         工序编码
 * @param processName         工序名称
 * @param processType         工序类型
 * @param standardTimeSeconds 标准工时，单位秒
 * @param keyProcess          是否关键工序
 * @param qualityRequired     是否需要质检
 * @param scanRequired        是否需要扫码
 * @param pieceRateEnabled    是否参与计件
 * @param equipmentCategoryId 设备类别 id
 * @param qualityPlanId       检验方案 id
 * @param remark              备注
 * @param status              启停状态
 * @param version             乐观锁版本
 * @author 张竹灏
 * @date 2026/07/10
 */
public record CraftProcessSnapshotDTO(
        String processCode,
        String processName,
        String processType,
        Integer standardTimeSeconds,
        Boolean keyProcess,
        Boolean qualityRequired,
        Boolean scanRequired,
        Boolean pieceRateEnabled,
        Long equipmentCategoryId,
        Long qualityPlanId,
        String remark,
        Integer status,
        Integer version) {
}
