package com.badminton.mes.module.quality.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanPageReqVO;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionPlanEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/** 检验标准方案动态查询条件。 */
public final class QualityInspectionPlanSpecifications {

    public static Specification<QualityInspectionPlanEntity> page(QualityInspectionPlanPageReqVO request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(request.getKeyword())) {
                String pattern = "%" + escapeWildcards(request.getKeyword()) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("planCode"), pattern, '\\'),
                        criteriaBuilder.like(root.get("planName"), pattern, '\\')));
            }
            if (request.getProductId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("productId"), request.getProductId()));
            }
            if (request.getCustomerId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("customerId"), request.getCustomerId()));
            }
            if (StringUtils.hasText(request.getInspectionType())) {
                predicates.add(criteriaBuilder.equal(root.get("inspectionType"), request.getInspectionType()));
            }
            if (StringUtils.hasText(request.getPlanStatus())) {
                predicates.add(criteriaBuilder.equal(root.get("planStatus"), request.getPlanStatus()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String escapeWildcards(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private QualityInspectionPlanSpecifications() {
    }
}
