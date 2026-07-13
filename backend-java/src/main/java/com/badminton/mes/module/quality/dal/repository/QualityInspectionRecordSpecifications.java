package com.badminton.mes.module.quality.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordPageReqVO;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionRecordEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/** 质量检验单动态查询条件。 */
public final class QualityInspectionRecordSpecifications {

    public static Specification<QualityInspectionRecordEntity> page(QualityInspectionRecordPageReqVO request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(request.getKeyword())) {
                String pattern = "%" + escapeWildcards(request.getKeyword()) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("inspectionNo"), pattern, '\\'),
                        criteriaBuilder.like(root.get("sourceDocumentNo"), pattern, '\\'),
                        criteriaBuilder.like(root.get("batchNo"), pattern, '\\')));
            }
            if (StringUtils.hasText(request.getInspectionType())) {
                predicates.add(criteriaBuilder.equal(root.get("inspectionType"), request.getInspectionType()));
            }
            if (StringUtils.hasText(request.getRecordStatus())) {
                predicates.add(criteriaBuilder.equal(root.get("recordStatus"), request.getRecordStatus()));
            }
            if (StringUtils.hasText(request.getConclusion())) {
                predicates.add(criteriaBuilder.equal(root.get("conclusion"), request.getConclusion()));
            }
            if (request.getWorkOrderId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("workOrderId"), request.getWorkOrderId()));
            }
            if (request.getProductId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("productId"), request.getProductId()));
            }
            if (StringUtils.hasText(request.getBatchNo())) {
                predicates.add(criteriaBuilder.equal(root.get("batchNo"), request.getBatchNo()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String escapeWildcards(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private QualityInspectionRecordSpecifications() {
    }
}
