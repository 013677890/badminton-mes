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
 * @author 张竹灏
 * @date 2026/07/14
 */
@Service
public class CompletionOrderPublishServiceImpl implements CompletionOrderPublishService {

    private static final int AUDIT_STATUS_APPROVED = 1;

    private final CompletionOrderRepository completionOrderRepository;

    public CompletionOrderPublishServiceImpl(CompletionOrderRepository completionOrderRepository) {
        this.completionOrderRepository = completionOrderRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishApproved(ApprovedCompletionDTO completion) {
        return completionOrderRepository.findByCompletionNoAndDeletedFalse(completion.completionNo())
                .map(CompletionOrderEntity::getId)
                .orElseGet(() -> saveNew(completion));
    }

    private Long saveNew(ApprovedCompletionDTO source) {
        CompletionOrderEntity entity = new CompletionOrderEntity();
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
        entity.setAuditStatus(AUDIT_STATUS_APPROVED);
        entity.setAuditBy(source.auditBy());
        entity.setAuditTime(source.auditTime());
        entity.setAuditRemark(source.auditRemark());
        entity.setCreateBy(source.auditBy());
        entity.setUpdateBy(source.auditBy());
        entity.setDeleted(false);
        try {
            return completionOrderRepository.saveAndFlush(entity).getId();
        } catch (DataIntegrityViolationException exception) {
            return completionOrderRepository.findByCompletionNoAndDeletedFalse(source.completionNo())
                    .map(CompletionOrderEntity::getId)
                    .orElseThrow(() -> exception);
        }
    }
}
