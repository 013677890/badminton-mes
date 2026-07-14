package com.badminton.mes.module.production.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.production.controller.vo.DispatchPageReqVO;
import com.badminton.mes.module.production.dal.entity.DispatchOrderEntity;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

/**
 * 派工单动态查询条件。
 *
 * <p>产线+日期+班次筛选可命中组合索引 idx_line_date_shift。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public final class DispatchOrderSpecifications {

    /**
     * 构造分页筛选条件。
     *
     * @param reqVO 分页请求
     * @return JPA Specification
     */
    public static Specification<DispatchOrderEntity> page(DispatchPageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (reqVO.getWorkOrderId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("workOrderId"), reqVO.getWorkOrderId()));
            }
            if (reqVO.getLineId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("lineId"), reqVO.getLineId()));
            }
            if (reqVO.getShiftId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("shiftId"), reqVO.getShiftId()));
            }
            if (reqVO.getDispatchStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("dispatchStatus"), reqVO.getDispatchStatus()));
            }
            if (reqVO.getPlanDateBegin() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("planDate"), reqVO.getPlanDateBegin()));
            }
            if (reqVO.getPlanDateEnd() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("planDate"), reqVO.getPlanDateEnd()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private DispatchOrderSpecifications() {
    }
}
