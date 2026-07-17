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
            // 动态条件只拼接请求中实际提供的字段，并始终保留逻辑删除过滤，保证 count 与列表口径一致。
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
                // 起始日期使用闭区间，包含用户选择的第一天。
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("planDate"), reqVO.getPlanDateBegin()));
            }
            if (reqVO.getPlanDateEnd() != null) {
                // 结束日期同样使用闭区间，包含用户选择的最后一天。
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("planDate"), reqVO.getPlanDateEnd()));
            }
            // 所有筛选条件使用 AND，避免不同条件之间产生超出页面预期的 OR 结果集。
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private DispatchOrderSpecifications() {
        // 仅提供静态 Specification 工厂，不允许实例化。
    }
}
