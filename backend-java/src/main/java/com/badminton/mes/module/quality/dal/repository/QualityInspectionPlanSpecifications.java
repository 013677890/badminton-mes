package com.badminton.mes.module.quality.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanPageReqVO;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionPlanEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 检验标准方案版本分页查询的动态条件构造器。
 *
 * <p>固定排除逻辑删除版本；产品、客户、检验类型和版本状态采用精确匹配，关键词在方案编码与名称
 * 之间执行 OR 模糊匹配，所有已提供的条件最终以 AND 组合。
 */
public final class QualityInspectionPlanSpecifications {

    /** 根据分页请求动态拼装方案适用范围、状态及关键词条件。 */
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

    /** 转义 LIKE 元字符，保证关键词中的特殊字符按字面值查询。 */
    private static String escapeWildcards(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private QualityInspectionPlanSpecifications() {
    }
}
