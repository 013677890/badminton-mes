package com.badminton.mes.module.equipment.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderPageReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentRepairOrderEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 设备报修任务动态查询条件。
 *
 * @author 角色C
 * @date 2026/07/10
 */
public final class EquipmentRepairOrderSpecifications {

    /**
     * 构造分页筛选条件。
     *
     * @param reqVO 分页请求
     * @return JPA Specification
     */
    public static Specification<EquipmentRepairOrderEntity> page(EquipmentRepairOrderPageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (StringUtils.hasText(reqVO.getKeyword())) {
                String escapedKeyword = escapeWildcards(reqVO.getKeyword());
                String pattern = "%" + escapedKeyword + "%";
                Predicate repairNoLike = criteriaBuilder.like(root.get("repairNo"), pattern, '\\');
                Predicate descriptionLike = criteriaBuilder.like(root.get("faultDescription"), pattern, '\\');
                Predicate resultLike = criteriaBuilder.like(root.get("repairResult"), pattern, '\\');
                predicates.add(criteriaBuilder.or(repairNoLike, descriptionLike, resultLike));
            }

            if (reqVO.getEquipmentId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("equipmentId"), reqVO.getEquipmentId()));
            }

            if (reqVO.getFaultPrincipleId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("faultPrincipleId"), reqVO.getFaultPrincipleId()));
            }

            if (StringUtils.hasText(reqVO.getRepairStatus())) {
                predicates.add(criteriaBuilder.equal(root.get("repairStatus"), reqVO.getRepairStatus()));
            }

            if (reqVO.getReportStartTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("reportTime"), reqVO.getReportStartTime()));
            }

            if (reqVO.getReportEndTime() != null) {
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

    private EquipmentRepairOrderSpecifications() {
    }
}
