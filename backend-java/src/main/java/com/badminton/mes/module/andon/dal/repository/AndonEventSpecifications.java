package com.badminton.mes.module.andon.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.andon.controller.vo.AndonEventPageReqVO;
import com.badminton.mes.module.andon.dal.entity.AndonEventEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/** 现场安灯异常动态查询条件。 */
public final class AndonEventSpecifications {

    public static Specification<AndonEventEntity> page(AndonEventPageReqVO request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(request.getKeyword())) {
                String pattern = "%" + escapeWildcards(request.getKeyword()) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("eventNo"), pattern, '\\'),
                        criteriaBuilder.like(root.get("description"), pattern, '\\'),
                        criteriaBuilder.like(root.get("batchNo"), pattern, '\\')));
            }
            if (request.getAndonTypeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("andonTypeId"), request.getAndonTypeId()));
            }
            if (request.getProductionLineId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("productionLineId"), request.getProductionLineId()));
            }
            if (request.getEquipmentId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("equipmentId"), request.getEquipmentId()));
            }
            if (request.getInitiatedBy() != null) {
                predicates.add(criteriaBuilder.equal(root.get("initiatedBy"), request.getInitiatedBy()));
            }
            if (request.getAssignedUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("assignedUserId"), request.getAssignedUserId()));
            }
            if (StringUtils.hasText(request.getAssignedRoleCode())) {
                predicates.add(criteriaBuilder.equal(root.get("assignedRoleCode"), request.getAssignedRoleCode()));
            }
            if (StringUtils.hasText(request.getSourceChannel())) {
                predicates.add(criteriaBuilder.equal(root.get("sourceChannel"), request.getSourceChannel()));
            }
            if (StringUtils.hasText(request.getEventStatus())) {
                predicates.add(criteriaBuilder.equal(root.get("eventStatus"), request.getEventStatus()));
            }
            if (StringUtils.hasText(request.getSeverity())) {
                predicates.add(criteriaBuilder.equal(root.get("severity"), request.getSeverity()));
            }
            if (StringUtils.hasText(request.getTimeoutStatus())) {
                predicates.add(criteriaBuilder.equal(root.get("timeoutStatus"), request.getTimeoutStatus()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String escapeWildcards(String input) {
        return input.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private AndonEventSpecifications() {
    }
}
