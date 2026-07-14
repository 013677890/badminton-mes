package com.badminton.mes.module.equipment.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenancePlanPageReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentMaintenancePlanEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/** 设备保养计划动态查询条件。 */
public final class EquipmentMaintenancePlanSpecifications {

    public static Specification<EquipmentMaintenancePlanEntity> page(EquipmentMaintenancePlanPageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (StringUtils.hasText(reqVO.getKeyword())) {
                String pattern = "%" + escapeWildcards(reqVO.getKeyword()) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("planCode"), pattern, '\\'),
                        criteriaBuilder.like(root.get("planName"), pattern, '\\'),
                        criteriaBuilder.like(root.get("maintenanceContent"), pattern, '\\')));
            }
            if (reqVO.getEquipmentId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("equipmentId"), reqVO.getEquipmentId()));
            }
            if (StringUtils.hasText(reqVO.getMaintenanceType())) {
                predicates.add(criteriaBuilder.equal(root.get("maintenanceType"), reqVO.getMaintenanceType()));
            }
            if (reqVO.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), reqVO.getStatus()));
            }
            if (reqVO.getNextMaintenanceStartTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("nextMaintenanceTime"), reqVO.getNextMaintenanceStartTime()));
            }
            if (reqVO.getNextMaintenanceEndTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("nextMaintenanceTime"), reqVO.getNextMaintenanceEndTime()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String escapeWildcards(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private EquipmentMaintenancePlanSpecifications() {
    }
}
