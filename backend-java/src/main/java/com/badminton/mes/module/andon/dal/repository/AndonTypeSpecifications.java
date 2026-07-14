package com.badminton.mes.module.andon.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.andon.controller.vo.AndonTypePageReqVO;
import com.badminton.mes.module.andon.dal.entity.AndonTypeEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/** 安灯类型动态查询条件。 */
public final class AndonTypeSpecifications {

    public static Specification<AndonTypeEntity> page(AndonTypePageReqVO request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(request.getKeyword())) {
                String pattern = "%" + escapeWildcards(request.getKeyword()) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("typeCode"), pattern, '\\'),
                        criteriaBuilder.like(root.get("typeName"), pattern, '\\')));
            }
            if (StringUtils.hasText(request.getExceptionCategory())) {
                predicates.add(criteriaBuilder.equal(root.get("exceptionCategory"), request.getExceptionCategory()));
            }
            if (StringUtils.hasText(request.getHandlingMode())) {
                predicates.add(criteriaBuilder.equal(root.get("handlingMode"), request.getHandlingMode()));
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

    private AndonTypeSpecifications() {
    }
}
