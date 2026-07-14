package com.badminton.mes.module.quality.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategoryPageReqVO;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionCategoryEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/** 检验分类动态查询条件。 */
public final class QualityInspectionCategorySpecifications {

    public static Specification<QualityInspectionCategoryEntity> page(
            QualityInspectionCategoryPageReqVO request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(request.getKeyword())) {
                String pattern = "%" + escapeWildcards(request.getKeyword()) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("categoryCode"), pattern, '\\'),
                        criteriaBuilder.like(root.get("categoryName"), pattern, '\\')));
            }
            if (request.getEnabledStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabledStatus"), request.getEnabledStatus()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String escapeWildcards(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private QualityInspectionCategorySpecifications() {
    }
}
