package com.badminton.mes.module.quality.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategoryPageReqVO;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionCategoryEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 检验分类分页查询的动态条件构造器。
 *
 * <p>查询固定排除逻辑删除数据，并将请求中实际提供的条件以 AND 连接；关键词内部则对分类编码、
 * 分类名称执行 OR 模糊匹配，启停状态采用精确匹配。
 */
public final class QualityInspectionCategorySpecifications {

    /** 根据分页请求按需拼装逻辑删除、关键词和启停状态条件。 */
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

    /** 转义 LIKE 元字符，使用户输入的百分号、下划线和反斜杠按普通文本参与匹配。 */
    private static String escapeWildcards(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private QualityInspectionCategorySpecifications() {
    }
}
