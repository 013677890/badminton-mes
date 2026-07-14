package com.badminton.mes.module.device.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigPageReqVO;
import com.badminton.mes.module.device.dal.entity.DeviceAccessConfigEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/** 设备接入配置动态查询条件。 */
public final class DeviceAccessConfigSpecifications {

    public static Specification<DeviceAccessConfigEntity> page(DeviceAccessConfigPageReqVO request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (StringUtils.hasText(request.getKeyword())) {
                String pattern = "%" + escapeWildcards(request.getKeyword()) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("configCode"), pattern, '\\'),
                        criteriaBuilder.like(root.get("configName"), pattern, '\\'),
                        criteriaBuilder.like(root.get("collectionPointCode"), pattern, '\\')));
            }
            if (request.getEquipmentId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("equipmentId"), request.getEquipmentId()));
            }
            if (request.getProcessId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("processId"), request.getProcessId()));
            }
            if (StringUtils.hasText(request.getCommissioningStatus())) {
                predicates.add(criteriaBuilder.equal(
                        root.get("commissioningStatus"), request.getCommissioningStatus()));
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

    private DeviceAccessConfigSpecifications() {
    }
}
