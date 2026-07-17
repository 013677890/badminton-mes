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
            // 每次查询都排除逻辑删除数据，确保列表、统计和详情读取使用同一数据口径。
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(reqVO.getWorkOrderNo())) {
                // 工单号采用前缀匹配，支持输入完整工单号或日期/流水号前缀进行快速筛选。
                predicates.add(criteriaBuilder.like(root.get("workOrderNo"), reqVO.getWorkOrderNo() + "%"));
            }
            if (reqVO.getWorkshopId() != null) {
                // 只有明确选择车间时才追加条件；为空表示查询全部授权车间。
                predicates.add(criteriaBuilder.equal(root.get("workshopId"), reqVO.getWorkshopId()));
            }
            if (reqVO.getOrderStatus() != null) {
                // 状态是等值筛选，不在数据库层把多个状态隐式合并，避免页面口径不清。
                predicates.add(criteriaBuilder.equal(root.get("orderStatus"), reqVO.getOrderStatus()));
            }
            if (reqVO.getPlanEndTimeBegin() != null) {
                // 起始时间使用大于等于，覆盖用户选择日期当天的零点边界。
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("planEndTime"), reqVO.getPlanEndTimeBegin()));
            }
            if (reqVO.getPlanEndTimeEnd() != null) {
                // 结束时间使用小于等于，配合前端补齐的 23:59:59 覆盖完整结束日期。
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("planEndTime"), reqVO.getPlanEndTimeEnd()));
            }
            // 统一使用 AND 合并条件；没有可选条件时仍保留 deleted=false 的基础约束。
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private WorkOrderSpecifications() {
    }
}
