package com.badminton.mes.module.andon.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.andon.controller.vo.AndonConfigurationPageReqVO;
import com.badminton.mes.module.andon.dal.entity.AndonConfigurationEntity;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

/** 安灯异常处理配置动态查询条件。 */
public final class AndonConfigurationSpecifications {

    public static Specification<AndonConfigurationEntity> page(AndonConfigurationPageReqVO request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (request.getAndonTypeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("andonTypeId"), request.getAndonTypeId()));
            }
            if (request.getProductionLineId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("productionLineId"), request.getProductionLineId()));
            }
            if (request.getHandlerUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("handlerUserId"), request.getHandlerUserId()));
            }
            if (request.getEnabledStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabledStatus"), request.getEnabledStatus()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AndonConfigurationSpecifications() {
    }
}
