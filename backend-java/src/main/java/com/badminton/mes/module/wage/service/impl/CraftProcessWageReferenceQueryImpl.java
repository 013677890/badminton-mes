package com.badminton.mes.module.wage.service.impl;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.module.craft.service.CraftProcessWageReferenceQuery;
import com.badminton.mes.module.wage.dal.repository.PieceRateRuleRepository;

import org.springframework.stereotype.Component;

/** 工资模块提供的工序计件规则反向引用查询实现。 */
@Component
public class CraftProcessWageReferenceQueryImpl implements CraftProcessWageReferenceQuery {

    private final PieceRateRuleRepository ruleRepository;

    /**
     * 构造器注入。
     *
     * @param ruleRepository 计件规则 Repository
     */
    public CraftProcessWageReferenceQueryImpl(PieceRateRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Override
    public boolean hasEnabledPieceRateRule(Long processId) {
        return ruleRepository.existsByProcessIdAndStatusAndDeletedFalse(
                processId, CommonStatusEnum.ENABLED.getStatus());
    }
}
