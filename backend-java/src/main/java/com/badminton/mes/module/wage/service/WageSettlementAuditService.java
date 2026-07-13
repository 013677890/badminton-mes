package com.badminton.mes.module.wage.service;

import com.badminton.mes.module.wage.dal.entity.WageSettlementAuditLogEntity;
import com.badminton.mes.module.wage.dal.repository.WageSettlementAuditLogRepository;
import com.badminton.mes.module.wage.enums.WageSettlementActionEnum;

import org.springframework.stereotype.Component;

/** 工资结算审计日志写入组件。 */
@Component
public class WageSettlementAuditService {

    private final WageSettlementAuditLogRepository auditLogRepository;

    /** 构造器注入。 */
    public WageSettlementAuditService(WageSettlementAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** 记录批次状态或计算动作。 */
    public void recordSettlement(Long settlementId, WageSettlementActionEnum action,
                                 Integer fromStatus, Integer toStatus, String reason, Long operatorId) {
        record(settlementId, null, action, fromStatus, toStatus, null, null, reason, operatorId);
    }

    /** 记录明细金额调整动作。 */
    public void recordAdjustment(Long settlementId, Long detailId, Long beforeAmount, Long afterAmount,
                                 String reason, Long operatorId) {
        record(settlementId, detailId, WageSettlementActionEnum.ADJUST,
                null, null, beforeAmount, afterAmount, reason, operatorId);
    }

    /** 创建审计日志实体。 */
    private void record(Long settlementId, Long detailId, WageSettlementActionEnum action,
                        Integer fromStatus, Integer toStatus, Long beforeAmount, Long afterAmount,
                        String reason, Long operatorId) {
        WageSettlementAuditLogEntity log = new WageSettlementAuditLogEntity();
        log.setSettlementId(settlementId);
        log.setDetailId(detailId);
        log.setActionType(action.name());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setBeforeAmountBasis(beforeAmount);
        log.setAfterAmountBasis(afterAmount);
        log.setActionReason(reason);
        log.setOperateBy(operatorId);
        auditLogRepository.save(log);
    }
}
