package com.badminton.mes.module.wage.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.wage.controller.vo.PieceRateRulePageReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementPageReqVO;
import com.badminton.mes.module.wage.controller.vo.WageWorkRecordPageReqVO;
import com.badminton.mes.module.wage.dal.entity.PieceRateRuleEntity;
import com.badminton.mes.module.wage.dal.entity.WageSettlementEntity;
import com.badminton.mes.module.wage.dal.entity.WageWorkRecordEntity;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

/** 计件工资动态分页查询条件。 */
public final class WageSpecifications {

    /** 构造计件规则分页条件。 */
    public static Specification<PieceRateRuleEntity> rulePage(PieceRateRulePageReqVO reqVO) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.isFalse(root.get("deleted")));
            if (reqVO.getProcessId() != null) {
                predicates.add(builder.equal(root.get("processId"), reqVO.getProcessId()));
            }
            if (reqVO.getProductId() != null) {
                predicates.add(builder.equal(root.get("productId"), reqVO.getProductId()));
            }
            if (reqVO.getStatus() != null) {
                predicates.add(builder.equal(root.get("status"), reqVO.getStatus()));
            }
            if (reqVO.getEffectiveDate() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("effectiveStart"), reqVO.getEffectiveDate()));
                predicates.add(builder.or(builder.isNull(root.get("effectiveEnd")),
                        builder.greaterThanOrEqualTo(root.get("effectiveEnd"), reqVO.getEffectiveDate())));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** 构造报工快照分页条件。 */
    public static Specification<WageWorkRecordEntity> workRecordPage(WageWorkRecordPageReqVO reqVO) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.isFalse(root.get("deleted")));
            addEqual(predicates, builder, root, "employeeId", reqVO.getEmployeeId());
            addEqual(predicates, builder, root, "workOrderId", reqVO.getWorkOrderId());
            addEqual(predicates, builder, root, "processId", reqVO.getProcessId());
            addEqual(predicates, builder, root, "productId", reqVO.getProductId());
            if (reqVO.getWorkDateBegin() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("workDate"), reqVO.getWorkDateBegin()));
            }
            if (reqVO.getWorkDateEnd() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("workDate"), reqVO.getWorkDateEnd()));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** 构造结算批次分页条件。 */
    public static Specification<WageSettlementEntity> settlementPage(WageSettlementPageReqVO reqVO) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.isFalse(root.get("deleted")));
            if (reqVO.getSettlementStatus() != null) {
                predicates.add(builder.equal(root.get("settlementStatus"), reqVO.getSettlementStatus()));
            }
            if (reqVO.getPeriodStartBegin() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("periodStart"), reqVO.getPeriodStartBegin()));
            }
            if (reqVO.getPeriodEndEnd() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("periodEnd"), reqVO.getPeriodEndEnd()));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addEqual(List<Predicate> predicates,
                                 jakarta.persistence.criteria.CriteriaBuilder builder,
                                 jakarta.persistence.criteria.Root<WageWorkRecordEntity> root,
                                 String field, Long value) {
        if (value != null) {
            predicates.add(builder.equal(root.get(field), value));
        }
    }

    private WageSpecifications() {
    }
}
