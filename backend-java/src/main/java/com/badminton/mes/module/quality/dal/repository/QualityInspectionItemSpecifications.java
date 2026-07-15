package com.badminton.mes.module.quality.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemPageReqVO;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionItemEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 检验项目分页查询的动态条件构造器。
 *
 * <p>始终过滤逻辑删除项目；请求中非空的分类、值类型、必检标记和启停状态均以 AND 精确匹配，
 * 关键词则在项目编码与名称之间执行 OR 模糊匹配。
 */
public final class QualityInspectionItemSpecifications {

    /** 根据分页请求按需组合关键词及各主数据属性过滤条件。 */
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

    /** 转义 LIKE 元字符，防止用户输入被解释为额外通配范围。 */
    private static String escapeWildcards(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private QualityInspectionItemSpecifications() {
    }
}
