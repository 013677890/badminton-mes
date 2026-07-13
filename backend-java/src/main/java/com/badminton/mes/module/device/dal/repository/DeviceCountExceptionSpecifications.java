package com.badminton.mes.module.device.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.device.controller.vo.DeviceCountExceptionPageReqVO;
import com.badminton.mes.module.device.dal.entity.DeviceCountExceptionEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/** 设备计数异常动态查询条件。 */
public final class DeviceCountExceptionSpecifications {

    public static Specification<DeviceCountExceptionEntity> page(DeviceCountExceptionPageReqVO request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (request.getAccessConfigId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("accessConfigId"), request.getAccessConfigId()));
            }
            if (request.getEquipmentId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("equipmentId"), request.getEquipmentId()));
            }
            if (StringUtils.hasText(request.getProcessingStatus())) {
                predicates.add(criteriaBuilder.equal(
                        root.get("processingStatus"), request.getProcessingStatus()));
            }
            if (request.getCreateStartTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createTime"), request.getCreateStartTime()));
            }
            if (request.getCreateEndTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createTime"), request.getCreateEndTime()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private DeviceCountExceptionSpecifications() {
    }
}
