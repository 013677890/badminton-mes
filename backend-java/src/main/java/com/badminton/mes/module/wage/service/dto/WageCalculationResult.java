package com.badminton.mes.module.wage.service.dto;

import java.math.BigDecimal;
import java.util.List;

import com.badminton.mes.module.wage.dal.entity.WageSettlementDetailEntity;

/** 单批次计件计算结果。 */
public record WageCalculationResult(List<WageSettlementDetailEntity> details,
                                    BigDecimal totalQualifiedQuantity,
                                    BigDecimal totalDefectQuantity,
                                    Long totalAmountBasis) {
}
