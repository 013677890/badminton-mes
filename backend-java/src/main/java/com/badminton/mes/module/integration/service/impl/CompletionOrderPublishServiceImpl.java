package com.badminton.mes.module.integration.service.impl;

import com.badminton.mes.module.integration.dal.entity.CompletionOrderEntity;
import com.badminton.mes.module.integration.dal.repository.CompletionOrderRepository;
import com.badminton.mes.module.integration.service.CompletionOrderPublishService;
import com.badminton.mes.module.integration.service.dto.ApprovedCompletionDTO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审核通过完工单的本地集成发布实现。
 *
 * <p>生产模块审核完工后，将审核时点的产品、数量和工单信息复制为集成侧可读取快照。完工单号
 * 作为幂等键：常规重复调用直接返回原主键，并发首次发布由数据库唯一约束裁决，落败事务随后
 * 回查获胜记录，从而不向审核主流程暴露无害的并发重复。
 *
 * @author 张竹灏
 * @date 2026/07/14
 */
@Service
public class CompletionOrderPublishServiceImpl implements CompletionOrderPublishService {

    /** 集成侧完工快照的审核通过状态固定值。 */
    private static final int AUDIT_STATUS_APPROVED = 1;

    /** 完工单集成仓储，用于按完工单号幂等查询和快照落库。 */
    private final CompletionOrderRepository completionOrderRepository;

    /** 构造本地完工发布服务。 */
    public CompletionOrderPublishServiceImpl(CompletionOrderRepository completionOrderRepository) {
        this.completionOrderRepository = completionOrderRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishApproved(ApprovedCompletionDTO completion) {
        // 前置查重处理绝大多数重复发布，直接沿用首次生成的集成快照主键。
        return completionOrderRepository.findByCompletionNoAndDeletedFalse(completion.completionNo())
                .map(CompletionOrderEntity::getId)
                .orElseGet(() -> saveNew(completion));
    }

    /** 将审核通过 DTO 完整复制为只供外部读取的完工快照，并处理并发幂等竞争。 */
    private Long saveNew(ApprovedCompletionDTO source) {
        CompletionOrderEntity entity = new CompletionOrderEntity();
        // 复制业务标识与产品快照，避免后续生产主档修改改变已经发布的完工数据。
        entity.setCompletionNo(source.completionNo());
        entity.setProductionTaskId(source.productionTaskId());
        entity.setWorkOrderId(source.workOrderId());
        entity.setWorkOrderNo(source.workOrderNo());
        entity.setProductId(source.productId());
        entity.setProductCode(source.productCode());
        entity.setProductName(source.productName());
        entity.setBatchNo(source.batchNo());
        entity.setCompletionQuantity(source.completionQuantity());
        entity.setGoodQuantity(source.goodQuantity());
        entity.setDefectQuantity(source.defectQuantity());
        // 本发布入口仅接收审核通过事件，状态不允许由调用方自由指定。
        entity.setAuditStatus(AUDIT_STATUS_APPROVED);
        entity.setAuditBy(source.auditBy());
        entity.setAuditTime(source.auditTime());
        entity.setAuditRemark(source.auditRemark());
        entity.setCreateBy(source.auditBy());
        entity.setUpdateBy(source.auditBy());
        entity.setDeleted(false);
        try {
            // 立即刷新以在方法内捕获两个事务同时发布同一完工单号的唯一约束冲突。
            return completionOrderRepository.saveAndFlush(entity).getId();
        } catch (DataIntegrityViolationException exception) {
            // 若冲突来自并发获胜事务，回查并返回其主键；确实查不到时保留原数据库异常。
            return completionOrderRepository.findByCompletionNoAndDeletedFalse(source.completionNo())
                    .map(CompletionOrderEntity::getId)
                    .orElseThrow(() -> exception);
        }
    }
}
