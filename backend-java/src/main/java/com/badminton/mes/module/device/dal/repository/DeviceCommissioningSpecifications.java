package com.badminton.mes.module.device.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.device.controller.vo.DeviceCommissioningPageReqVO;
import com.badminton.mes.module.device.dal.entity.DeviceCommissioningRecordEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/** 设备联调记录动态查询条件。 */
public final class DeviceCommissioningSpecifications {

    public static Specification<DeviceCommissioningRecordEntity> page(DeviceCommissioningPageReqVO request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (request.getAccessConfigId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("accessConfigId"), request.getAccessConfigId()));
            }
            if (StringUtils.hasText(request.getTestResult())) {
                predicates.add(criteriaBuilder.equal(root.get("testResult"), request.getTestResult()));
            }
            if (request.getTestStartTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("testTime"), request.getTestStartTime()));
            }
            if (request.getTestEndTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("testTime"), request.getTestEndTime()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private DeviceCommissioningSpecifications() {
    }
}
