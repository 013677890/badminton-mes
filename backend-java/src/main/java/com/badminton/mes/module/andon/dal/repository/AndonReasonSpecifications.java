package com.badminton.mes.module.andon.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.andon.controller.vo.AndonReasonPageReqVO;
import com.badminton.mes.module.andon.dal.entity.AndonReasonEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/** 安灯异常原因动态查询条件。 */
public final class AndonReasonSpecifications {

    public static Specification<AndonReasonEntity> page(AndonReasonPageReqVO request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(request.getKeyword())) {
                String pattern = "%" + escapeWildcards(request.getKeyword()) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("reasonCode"), pattern, '\\'),
                        criteriaBuilder.like(root.get("reasonName"), pattern, '\\')));
            }
            if (request.getAndonTypeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("andonTypeId"), request.getAndonTypeId()));
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

    private AndonReasonSpecifications() {
    }
}
