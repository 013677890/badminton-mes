package com.badminton.mes.module.production.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.production.controller.vo.WorkOrderPageReqVO;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 生产工单动态查询条件。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
public final class WorkOrderSpecifications {

    /**
     * 构造分页筛选条件。
     *
     * @param reqVO 分页请求
     * @return JPA Specification
     */
    public static Specification<WorkOrderEntity> page(WorkOrderPageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(reqVO.getWorkOrderNo())) {
                predicates.add(criteriaBuilder.like(root.get("workOrderNo"), reqVO.getWorkOrderNo() + "%"));
            }
            if (reqVO.getWorkshopId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("workshopId"), reqVO.getWorkshopId()));
            }
            if (reqVO.getOrderStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("orderStatus"), reqVO.getOrderStatus()));
            }
            if (reqVO.getPlanEndTimeBegin() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("planEndTime"), reqVO.getPlanEndTimeBegin()));
            }
            if (reqVO.getPlanEndTimeEnd() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("planEndTime"), reqVO.getPlanEndTimeEnd()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private WorkOrderSpecifications() {
    }
}
