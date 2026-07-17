package com.badminton.mes.module.barcode.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.barcode.controller.vo.BarcodeRulePageReqVO;
import com.badminton.mes.module.barcode.dal.entity.BarcodeRuleEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 条码规则动态查询条件。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public final class BarcodeRuleSpecifications {

    /**
     * 构造分页筛选条件：编码右模糊、名称包含、类型与状态相等，固定过滤已删除。
     *
     * @param reqVO 分页请求
     * @return JPA Specification
     */
    public static Specification<BarcodeRuleEntity> page(BarcodeRulePageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 所有动态条件最终均以 AND 连接，并始终排除逻辑删除数据。
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(reqVO.getRuleCode())) {
                // 编码采用右模糊匹配，保留数据库利用编码索引前缀过滤的可能性。
                predicates.add(criteriaBuilder.like(root.get("ruleCode"), reqVO.getRuleCode() + "%"));
            }
            if (StringUtils.hasText(reqVO.getRuleName())) {
                predicates.add(criteriaBuilder.like(root.get("ruleName"), "%" + reqVO.getRuleName() + "%"));
            }
            if (reqVO.getBarcodeTypeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("barcodeTypeId"), reqVO.getBarcodeTypeId()));
            }
            if (reqVO.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), reqVO.getStatus()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private BarcodeRuleSpecifications() {
    }
}
