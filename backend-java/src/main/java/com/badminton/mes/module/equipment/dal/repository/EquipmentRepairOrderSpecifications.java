package com.badminton.mes.module.equipment.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderPageReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentRepairOrderEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 设备报修任务分页查询的动态条件构造器。
 *
 * <p>规格始终排除逻辑删除任务；关键字在报修单号、故障描述和维修结果间执行 OR 包含匹配，
 * 设备、故障原理、报修状态和上报时间区间按非空入参追加精确或边界条件。所有条件组最终以
 * AND 连接，以支持设备维修履历和待办任务的复合筛选。
 *
 * @author 角色C
 * @date 2026/07/10
 */
public final class EquipmentRepairOrderSpecifications {

    /**
     * 构造设备报修任务分页筛选规格。
     *
     * <p>上报开始、结束时间分别形成闭区间下界和上界；调用方可只传一侧形成开放时间范围。
     * 返回规格不附加排序，报修时间排序由分页调用方决定。
     *
     * @param reqVO 分页请求
     * @return 可供报修 Repository 执行的组合查询条件
     */
    public static Specification<EquipmentRepairOrderEntity> page(EquipmentRepairOrderPageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 已逻辑删除任务不进入待办列表和设备维修履历。
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (StringUtils.hasText(reqVO.getKeyword())) {
                // 单号、故障现象或维修结论任一包含关键字即可命中。
                String escapedKeyword = escapeWildcards(reqVO.getKeyword());
                String pattern = "%" + escapedKeyword + "%";
                Predicate repairNoLike = criteriaBuilder.like(root.get("repairNo"), pattern, '\\');
                Predicate descriptionLike = criteriaBuilder.like(root.get("faultDescription"), pattern, '\\');
                Predicate resultLike = criteriaBuilder.like(root.get("repairResult"), pattern, '\\');
                predicates.add(criteriaBuilder.or(repairNoLike, descriptionLike, resultLike));
            }

            if (reqVO.getEquipmentId() != null) {
                // 精确限定报修设备，用于查询单台设备的维修任务。
                predicates.add(criteriaBuilder.equal(root.get("equipmentId"), reqVO.getEquipmentId()));
            }

            if (reqVO.getFaultPrincipleId() != null) {
                // 精确限定标准故障原理，便于同类故障统计与追溯。
                predicates.add(criteriaBuilder.equal(root.get("faultPrincipleId"), reqVO.getFaultPrincipleId()));
            }

            if (StringUtils.hasText(reqVO.getRepairStatus())) {
                // 状态按状态机枚举字符串精确过滤。
                predicates.add(criteriaBuilder.equal(root.get("repairStatus"), reqVO.getRepairStatus()));
            }

            if (reqVO.getReportStartTime() != null) {
                // 开始时间作为包含边界，仅保留在该时刻及之后上报的任务。
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("reportTime"), reqVO.getReportStartTime()));
            }

            if (reqVO.getReportEndTime() != null) {
                // 结束时间作为包含边界，仅保留在该时刻及之前上报的任务。
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("reportTime"), reqVO.getReportEndTime()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 转义 LIKE 查询中的通配符，防止用户输入的 % 和 _ 被当作通配符处理。
     *
     * @param input 用户输入
     * @return 转义后的字符串
     */
    private static String escapeWildcards(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
    }

    /** 纯静态条件构造器，不允许实例化。 */
    private EquipmentRepairOrderSpecifications() {
    }
}
