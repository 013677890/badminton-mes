package com.badminton.mes.module.quality.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordPageReqVO;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionRecordEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 质量检验单分页查询的动态条件构造器。
 *
 * <p>固定过滤逻辑删除单据；关键词在检验单号、来源单号与批次号之间执行 OR 模糊匹配，其余已提供的
 * 类型、状态、结论、工单、产品及批次条件采用精确匹配，并与关键词条件整体以 AND 组合。
 */
public final class QualityInspectionRecordSpecifications {

    /** 根据分页请求动态构造检验任务来源、执行状态及业务范围过滤条件。 */
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

    /** 转义 LIKE 元字符，避免用户输入扩大关键词模糊匹配范围。 */
    private static String escapeWildcards(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private QualityInspectionRecordSpecifications() {
    }
}
