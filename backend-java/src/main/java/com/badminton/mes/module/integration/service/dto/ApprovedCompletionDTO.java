package com.badminton.mes.module.integration.service.dto;

import java.time.LocalDateTime;

/**
 * B 组完工审核后发布给 A 组集成模块的稳定数据契约。
 *
 * <p>该 DTO 固化审核时的工单、产品、批次和数量信息，发布服务据此生成幂等的集成完工快照；
 * 不把生产实体直接传入 integration，避免跨模块持久化上下文和级联关系泄漏。
 *
 * @author 张竹灏
 * @date 2026/07/14
 */
public record ApprovedCompletionDTO(
                                    /** 审核通过完工单业务编号，作为发布幂等键。 */
                                    String completionNo,
                                    /** 现场生产任务主键。 */
                                    Long productionTaskId,
                                    /** 生产工单主键。 */
                                    Long workOrderId,
                                    /** 生产工单业务编号快照。 */
                                    String workOrderNo,
                                    /** 产品主键。 */
                                    Long productId,
                                    /** 产品编码快照。 */
                                    String productCode,
                                    /** 产品名称快照。 */
                                    String productName,
                                    /** 生产批次号。 */
                                    String batchNo,
                                    /** 审核通过总完工数量。 */
                                    Integer completionQuantity,
                                    /** 审核通过良品数量。 */
                                    Integer goodQuantity,
                                    /** 审核通过不良数量。 */
                                    Integer defectQuantity,
                                    /** 审核人用户主键。 */
                                    Long auditBy,
                                    /** 审核完成时间。 */
                                    LocalDateTime auditTime,
                                    /** 审核意见快照。 */
                                    String auditRemark) {
}
