package com.badminton.mes.module.quality.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemPageReqVO;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionItemEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/** 检验项目动态查询条件。 */
public final class QualityInspectionItemSpecifications {

    public static Specification<QualityInspectionItemEntity> page(QualityInspectionItemPageReqVO request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(request.getKeyword())) {
                String pattern = "%" + escapeWildcards(request.getKeyword()) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("itemCode"), pattern, '\\'),
                        criteriaBuilder.like(root.get("itemName"), pattern, '\\')));
            }
            if (request.getCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("categoryId"), request.getCategoryId()));
            }
            if (StringUtils.hasText(request.getValueType())) {
                predicates.add(criteriaBuilder.equal(root.get("valueType"), request.getValueType()));
            }
            if (request.getRequiredFlag() != null) {
                predicates.add(criteriaBuilder.equal(root.get("requiredFlag"), request.getRequiredFlag()));
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

    private QualityInspectionItemSpecifications() {
    }
}
