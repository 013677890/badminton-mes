package com.badminton.mes.module.device.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.device.controller.vo.DeviceCountRecordPageReqVO;
import com.badminton.mes.module.device.dal.entity.DeviceCountRecordEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/** 设备计数记录动态查询条件。 */
public final class DeviceCountRecordSpecifications {

    public static Specification<DeviceCountRecordEntity> page(DeviceCountRecordPageReqVO request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (request.getAccessConfigId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("accessConfigId"), request.getAccessConfigId()));
            }
            if (request.getEquipmentId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("equipmentId"), request.getEquipmentId()));
            }
            if (StringUtils.hasText(request.getMatchStatus())) {
                predicates.add(criteriaBuilder.equal(root.get("matchStatus"), request.getMatchStatus()));
            }
            if (request.getCollectedStartTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("collectedAt"), request.getCollectedStartTime()));
            }
            if (request.getCollectedEndTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("collectedAt"), request.getCollectedEndTime()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private DeviceCountRecordSpecifications() {
    }
}
