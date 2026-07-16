package com.badminton.mes.module.integration.service.dto;

import java.time.LocalDateTime;

/**
 * B 组完工审核后发布给 A 组集成模块的稳定数据契约。
 *
 * @author 张竹灏
 * @date 2026/07/14
 */
public record ApprovedCompletionDTO(String completionNo,
                                    Long productionTaskId,
                                    Long workOrderId,
                                    String workOrderNo,
                                    Long productId,
                                    String productCode,
                                    String productName,
                                    String batchNo,
                                    Integer completionQuantity,
                                    Integer goodQuantity,
                                    Integer defectQuantity,
                                    Long auditBy,
                                    LocalDateTime auditTime,
                                    String auditRemark) {
}
