package com.badminton.mes.module.wage.service.impl;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.module.wage.dal.repository.PieceRateRuleRepository;
import com.badminton.mes.module.wage.service.ProductPieceRateRuleReferenceQuery;

import org.springframework.stereotype.Service;

/**
 * 产品计件规则反向引用查询实现。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@Service
public class ProductPieceRateRuleReferenceQueryImpl
        implements ProductPieceRateRuleReferenceQuery {

    private final PieceRateRuleRepository ruleRepository;

    /**
     * 构造器注入。
     *
     * @param ruleRepository 计件规则 Repository
     */
    public ProductPieceRateRuleReferenceQueryImpl(PieceRateRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Override
    public boolean hasEnabledRule(Long productId) {
        return ruleRepository.existsByProductIdAndStatusAndDeletedFalse(
                productId, CommonStatusEnum.ENABLED.getStatus());
    }

    @Override
    public boolean hasAnyRule(Long productId) {
        return ruleRepository.existsByProductIdAndDeletedFalse(productId);
    }
}
