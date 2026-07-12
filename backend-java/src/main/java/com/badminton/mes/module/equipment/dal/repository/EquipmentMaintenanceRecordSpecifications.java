package com.badminton.mes.module.equipment.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordPageReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentMaintenanceRecordEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/** 设备保养记录动态查询条件。 */
public final class EquipmentMaintenanceRecordSpecifications {

    public static Specification<EquipmentMaintenanceRecordEntity> page(EquipmentMaintenanceRecordPageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (StringUtils.hasText(reqVO.getKeyword())) {
                String pattern = "%" + escapeWildcards(reqVO.getKeyword()) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("recordNo"), pattern, '\\'),
                        criteriaBuilder.like(root.get("maintenanceContent"), pattern, '\\'),
                        criteriaBuilder.like(root.get("abnormalDescription"), pattern, '\\')));
            }
            if (reqVO.getPlanId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("planId"), reqVO.getPlanId()));
            }
            if (reqVO.getEquipmentId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("equipmentId"), reqVO.getEquipmentId()));
            }
            if (StringUtils.hasText(reqVO.getRecordStatus())) {
                predicates.add(criteriaBuilder.equal(root.get("recordStatus"), reqVO.getRecordStatus()));
            }
            if (StringUtils.hasText(reqVO.getMaintenanceResult())) {
                predicates.add(criteriaBuilder.equal(root.get("maintenanceResult"), reqVO.getMaintenanceResult()));
            }
            if (reqVO.getScheduledStartTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("scheduledTime"), reqVO.getScheduledStartTime()));
            }
            if (reqVO.getScheduledEndTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("scheduledTime"), reqVO.getScheduledEndTime()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String escapeWildcards(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private EquipmentMaintenanceRecordSpecifications() {
    }
}
