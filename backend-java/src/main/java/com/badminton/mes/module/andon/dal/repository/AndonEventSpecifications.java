package com.badminton.mes.module.andon.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.andon.controller.vo.AndonEventPageReqVO;
import com.badminton.mes.module.andon.dal.entity.AndonEventEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 现场安灯异常分页动态查询条件。
 *
 * <p>所有可选条件均与逻辑删除过滤条件取 AND；关键词内部对事件单号、异常描述和批次号取 OR。
 */
public final class AndonEventSpecifications {

    /**
     * 根据分页请求构造事件筛选条件；请求中未提供的可选字段不会形成查询约束。
     *
     * @param request 事件分页筛选参数
     * @return 可与分页、排序组合执行的 JPA 查询条件
     */
    public static Specification<AndonEventEntity> page(AndonEventPageReqVO request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 列表查询固定排除逻辑删除事件，避免历史删除数据参与后续筛选。
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(request.getKeyword())) {
                // 关键词采用包含匹配，并转义通配符，确保用户输入的百分号和下划线按字面量检索。
                String pattern = "%" + escapeWildcards(request.getKeyword()) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("eventNo"), pattern, '\\'),
                        criteriaBuilder.like(root.get("description"), pattern, '\\'),
                        criteriaBuilder.like(root.get("batchNo"), pattern, '\\')));
            }
            // 业务上下文条件均为精确匹配，用于按类型、产线、设备及发起人缩小事件范围。
            if (request.getAndonTypeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("andonTypeId"), request.getAndonTypeId()));
            }
            if (request.getProductionLineId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("productionLineId"), request.getProductionLineId()));
            }
            if (request.getEquipmentId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("equipmentId"), request.getEquipmentId()));
            }
            if (request.getInitiatedBy() != null) {
                predicates.add(criteriaBuilder.equal(root.get("initiatedBy"), request.getInitiatedBy()));
            }
            // 当前责任主体可分别按明确指派用户或指派角色筛选。
            if (request.getAssignedUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("assignedUserId"), request.getAssignedUserId()));
            }
            if (StringUtils.hasText(request.getAssignedRoleCode())) {
                predicates.add(criteriaBuilder.equal(root.get("assignedRoleCode"), request.getAssignedRoleCode()));
            }
            // 来源、流程状态、严重程度和超时状态均按枚举值精确过滤，不执行模糊匹配。
            if (StringUtils.hasText(request.getSourceChannel())) {
                predicates.add(criteriaBuilder.equal(root.get("sourceChannel"), request.getSourceChannel()));
            }
            if (StringUtils.hasText(request.getEventStatus())) {
                predicates.add(criteriaBuilder.equal(root.get("eventStatus"), request.getEventStatus()));
            }
            if (StringUtils.hasText(request.getSeverity())) {
                predicates.add(criteriaBuilder.equal(root.get("severity"), request.getSeverity()));
            }
            if (StringUtils.hasText(request.getTimeoutStatus())) {
                predicates.add(criteriaBuilder.equal(root.get("timeoutStatus"), request.getTimeoutStatus()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** 将 LIKE 特殊字符转义为普通字符，配合查询中显式指定的反斜杠转义符使用。 */
    private static String escapeWildcards(String input) {
        return input.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private AndonEventSpecifications() {
    }
}
