package com.badminton.mes.module.wage.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.wage.constants.WageErrorCodeConstants;
import com.badminton.mes.module.wage.dal.entity.PieceRateRuleEntity;
import com.badminton.mes.module.wage.dal.entity.WageSettlementDetailEntity;
import com.badminton.mes.module.wage.dal.entity.WageWorkRecordEntity;
import com.badminton.mes.module.wage.dal.repository.PieceRateRuleRepository;
import com.badminton.mes.module.wage.service.dto.WageCalculationResult;
import com.badminton.mes.module.wage.service.support.WageAmountUtils;

import org.springframework.stereotype.Component;

/** 将已锁定报工快照按生效规则计算为结算明细。 */
@Component
public class WageSettlementCalculator {

    private final PieceRateRuleRepository ruleRepository;

    /** 构造器注入。 */
    public WageSettlementCalculator(PieceRateRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    /**
     * 计算结算明细和批次合计。
     *
     * @param settlementId 结算主键
     * @param records      已悲观锁定且未被占用的报工
     * @param periodStart  周期开始日期
     * @param periodEnd    周期结束日期
     * @return 计算结果
     */
    public WageCalculationResult calculate(Long settlementId, List<WageWorkRecordEntity> records,
                                            LocalDate periodStart, LocalDate periodEnd) {
        Set<Long> processIds = records.stream().map(WageWorkRecordEntity::getProcessId)
                .collect(Collectors.toSet());
        List<PieceRateRuleEntity> rules = ruleRepository.findCandidates(processIds,
                CommonStatusEnum.ENABLED.getStatus(), periodStart, periodEnd);
        Map<Long, List<PieceRateRuleEntity>> rulesByProcess = rules.stream()
                .collect(Collectors.groupingBy(PieceRateRuleEntity::getProcessId, HashMap::new, Collectors.toList()));
        rulesByProcess.values().forEach(list -> list.sort(
                Comparator.comparing(PieceRateRuleEntity::getEffectiveStart).reversed()
                        .thenComparing(PieceRateRuleEntity::getId).reversed()));

        List<WageSettlementDetailEntity> details = new ArrayList<>(records.size());
        BigDecimal qualifiedTotal = BigDecimal.ZERO;
        BigDecimal defectTotal = BigDecimal.ZERO;
        long amountTotal = 0L;
        for (WageWorkRecordEntity record : records) {
            PieceRateRuleEntity rule = matchRule(record, rulesByProcess.getOrDefault(
                    record.getProcessId(), List.of()));
            WageSettlementDetailEntity detail = buildDetail(settlementId, record, rule);
            details.add(detail);
            qualifiedTotal = qualifiedTotal.add(record.getQualifiedQuantity());
            defectTotal = defectTotal.add(record.getDefectQuantity());
            try {
                amountTotal = Math.addExact(amountTotal, detail.getFinalAmountBasis());
            } catch (ArithmeticException exception) {
                throw new ServiceException(WageErrorCodeConstants.AMOUNT_OUT_OF_RANGE);
            }
        }
        return new WageCalculationResult(details, qualifiedTotal, defectTotal, amountTotal);
    }

    /** 产品专用规则优先，未命中时回退工序通用规则。 */
    private PieceRateRuleEntity matchRule(WageWorkRecordEntity record, List<PieceRateRuleEntity> candidates) {
        PieceRateRuleEntity genericRule = null;
        for (PieceRateRuleEntity rule : candidates) {
            if (!isEffective(rule, record.getWorkDate())) {
                continue;
            }
            if (Objects.equals(rule.getProductId(), record.getProductId())) {
                return rule;
            }
            if (rule.getProductId() == null && genericRule == null) {
                genericRule = rule;
            }
        }
        if (genericRule == null) {
            throw new ServiceException(WageErrorCodeConstants.SETTLEMENT_RULE_MISSING,
                    "报工 " + record.getSourceReportId() + " 未匹配到有效计件规则");
        }
        return genericRule;
    }

    /** 判断规则在作业日期有效。 */
    private boolean isEffective(PieceRateRuleEntity rule, LocalDate workDate) {
        return !rule.getEffectiveStart().isAfter(workDate)
                && (rule.getEffectiveEnd() == null || !rule.getEffectiveEnd().isBefore(workDate));
    }

    /** 构造单条结算明细快照。 */
    private WageSettlementDetailEntity buildDetail(Long settlementId, WageWorkRecordEntity record,
                                                    PieceRateRuleEntity rule) {
        WageSettlementDetailEntity detail = new WageSettlementDetailEntity();
        detail.setSettlementId(settlementId);
        detail.setWorkRecordId(record.getId());
        detail.setRuleId(rule.getId());
        detail.setEmployeeId(record.getEmployeeId());
        detail.setWorkDate(record.getWorkDate());
        detail.setWorkOrderId(record.getWorkOrderId());
        detail.setProcessId(record.getProcessId());
        detail.setProductId(record.getProductId());
        detail.setQualifiedQuantity(record.getQualifiedQuantity());
        detail.setDefectQuantity(record.getDefectQuantity());
        detail.setUnitPriceBasis(rule.getUnitPriceBasis());
        detail.setDefectDeductionRate(rule.getDefectDeductionRate());
        long amount = WageAmountUtils.calculateAmount(record.getQualifiedQuantity(),
                record.getDefectQuantity(), rule.getUnitPriceBasis(), rule.getDefectDeductionRate());
        detail.setCalculatedAmountBasis(amount);
        detail.setFinalAmountBasis(amount);
        detail.setActive(true);
        return detail;
    }
}
