package com.badminton.mes.module.integration.service.dto;

/**
 * ERP 工艺工序步骤数据传输对象。
 *
 * @param sequenceNo  连续工序顺序号，从 1 开始
 * @param processCode 工序编码（用于在 MES 中查找工序主键）
 * @param processName 工序名称（ERP 侧名称，供展示）
 * @author 张竹灏
 * @date 2026/07/13
 */
public record ErpCraftStepDTO(Integer sequenceNo,
                              String processCode,
                              String processName) {
}
