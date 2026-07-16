package com.badminton.mes.module.equipment.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordPageReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentMaintenanceRecordEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 设备保养记录动态查询条件构造器。
 *
 * <p>集中拼装保养任务列表的可选过滤条件，并始终剔除逻辑删除记录。关键字同时检索任务编号、实际
 * 保养内容和异常说明，LIKE 特殊字符按普通文本处理；计划、设备、任务状态、保养结果以及计划
 * 执行时间区间按非空入参追加，并与关键字组使用 AND 连接。
 */
public final class EquipmentMaintenanceRecordSpecifications {

    /**
     * 构造保养记录分页查询条件。
     *
     * <p>计划执行开始和结束时间分别形成包含边界，可只指定一侧。返回规格仅负责 WHERE 条件，
     * 不改变分页大小和排序策略。
     *
     * @param reqVO 分页筛选请求
     * @return 可供 Repository 执行的动态查询规格
     */
    public static Specification<EquipmentMaintenanceRecordEntity> page(EquipmentMaintenanceRecordPageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 保养历史只展示当前有效记录，逻辑删除过滤必须成为规格的固定组成部分。
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (StringUtils.hasText(reqVO.getKeyword())) {
                // 任务编号、实际保养内容或异常说明任一包含关键字即可命中。
                String pattern = "%" + escapeWildcards(reqVO.getKeyword()) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("recordNo"), pattern, '\\'),
                        criteriaBuilder.like(root.get("maintenanceContent"), pattern, '\\'),
                        criteriaBuilder.like(root.get("abnormalDescription"), pattern, '\\')));
            }
            if (reqVO.getPlanId() != null) {
                // 按来源计划精确过滤，用于查看计划关联的全部任务历史。
                predicates.add(criteriaBuilder.equal(root.get("planId"), reqVO.getPlanId()));
            }
            if (reqVO.getEquipmentId() != null) {
                // 按实际执行设备精确过滤，用于形成单台设备保养履历。
                predicates.add(criteriaBuilder.equal(root.get("equipmentId"), reqVO.getEquipmentId()));
            }
            if (StringUtils.hasText(reqVO.getRecordStatus())) {
                // 任务状态按状态机枚举字符串精确匹配。
                predicates.add(criteriaBuilder.equal(root.get("recordStatus"), reqVO.getRecordStatus()));
            }
            if (StringUtils.hasText(reqVO.getMaintenanceResult())) {
                // 保养结论精确区分正常和异常，未完成任务通常没有该值。
                predicates.add(criteriaBuilder.equal(root.get("maintenanceResult"), reqVO.getMaintenanceResult()));
            }
            if (reqVO.getScheduledStartTime() != null) {
                // 包含起始时刻，仅保留在该时刻及之后计划执行的任务。
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("scheduledTime"), reqVO.getScheduledStartTime()));
            }
            if (reqVO.getScheduledEndTime() != null) {
                // 包含结束时刻，仅保留在该时刻及之前计划执行的任务。
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("scheduledTime"), reqVO.getScheduledEndTime()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 转义 SQL LIKE 通配符和转义符，避免用户输入改变包含匹配语义。
     *
     * @param input 非空关键字
     * @return 转义后的 LIKE 内容
     */
    private static String escapeWildcards(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** 工具类不允许实例化。 */
    private EquipmentMaintenanceRecordSpecifications() {
    }
}
