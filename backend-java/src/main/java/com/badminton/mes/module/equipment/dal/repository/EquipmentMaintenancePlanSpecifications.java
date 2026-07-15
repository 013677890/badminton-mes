package com.badminton.mes.module.equipment.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenancePlanPageReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentMaintenancePlanEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 设备保养计划动态查询条件构造器。
 *
 * <p>将可选请求条件组合为 JPA {@link Specification}，并无条件排除逻辑删除数据。关键字按字面值
 * 在计划编码、名称和标准保养内容间执行 OR 包含匹配；设备、保养类型、启停状态及下次保养
 * 时间区间按非空入参追加，并与关键字组使用 AND 连接。LIKE 特殊字符先转义，避免扩大查询范围。
 */
public final class EquipmentMaintenancePlanSpecifications {

    /**
     * 构造保养计划分页查询条件。
     *
     * <p>下次保养开始和结束时间分别形成包含边界，可单独使用一侧；返回规格仅描述过滤逻辑，
     * 具体分页和排序由 Repository 调用参数决定。
     *
     * @param reqVO 分页筛选请求
     * @return 可供 Repository 执行的动态查询规格
     */
    public static Specification<EquipmentMaintenancePlanEntity> page(EquipmentMaintenancePlanPageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 逻辑删除是所有正常查询的基础约束，不能依赖调用方自行补充。
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (StringUtils.hasText(reqVO.getKeyword())) {
                // 计划编码、名称或标准作业内容任一包含关键字即可命中。
                String pattern = "%" + escapeWildcards(reqVO.getKeyword()) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("planCode"), pattern, '\\'),
                        criteriaBuilder.like(root.get("planName"), pattern, '\\'),
                        criteriaBuilder.like(root.get("maintenanceContent"), pattern, '\\')));
            }
            if (reqVO.getEquipmentId() != null) {
                // 精确限定计划所绑定的设备台账。
                predicates.add(criteriaBuilder.equal(root.get("equipmentId"), reqVO.getEquipmentId()));
            }
            if (StringUtils.hasText(reqVO.getMaintenanceType())) {
                // 保养类型按枚举字符串精确匹配。
                predicates.add(criteriaBuilder.equal(root.get("maintenanceType"), reqVO.getMaintenanceType()));
            }
            if (reqVO.getStatus() != null) {
                // 计划启停状态非空时精确过滤，空值则同时返回启用和停用计划。
                predicates.add(criteriaBuilder.equal(root.get("status"), reqVO.getStatus()));
            }
            if (reqVO.getNextMaintenanceStartTime() != null) {
                // 包含起始时刻，用于查找该时刻及之后到期的计划。
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("nextMaintenanceTime"), reqVO.getNextMaintenanceStartTime()));
            }
            if (reqVO.getNextMaintenanceEndTime() != null) {
                // 包含结束时刻，用于查找截至该时刻应执行的计划。
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("nextMaintenanceTime"), reqVO.getNextMaintenanceEndTime()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 转义 SQL LIKE 中具有语义的字符，使关键字查询按用户输入的字面值匹配。
     *
     * @param input 非空关键字
     * @return 转义后的 LIKE 内容
     */
    private static String escapeWildcards(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** 工具类不允许实例化。 */
    private EquipmentMaintenancePlanSpecifications() {
    }
}
