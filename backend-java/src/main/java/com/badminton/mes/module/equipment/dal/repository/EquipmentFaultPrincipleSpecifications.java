package com.badminton.mes.module.equipment.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentFaultPrinciplePageReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentFaultPrincipleEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 设备故障原理动态查询条件。
 *
 * @author 角色C
 * @date 2026/07/10
 */
public final class EquipmentFaultPrincipleSpecifications {

    /**
     * 构造分页筛选条件。
     *
     * @param reqVO 分页请求
     * @return JPA Specification
     */
    public static Specification<EquipmentFaultPrincipleEntity> page(EquipmentFaultPrinciplePageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (StringUtils.hasText(reqVO.getKeyword())) {
                String escapedKeyword = escapeWildcards(reqVO.getKeyword());
                String pattern = "%" + escapedKeyword + "%";
                Predicate codeLike = criteriaBuilder.like(root.get("faultCode"), pattern, '\\');
                Predicate nameLike = criteriaBuilder.like(root.get("faultName"), pattern, '\\');
                Predicate descriptionLike = criteriaBuilder.like(root.get("faultDescription"), pattern, '\\');
                predicates.add(criteriaBuilder.or(codeLike, nameLike, descriptionLike));
            }

            if (reqVO.getCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("categoryId"), reqVO.getCategoryId()));
            }

            if (StringUtils.hasText(reqVO.getFaultLevel())) {
                predicates.add(criteriaBuilder.equal(root.get("faultLevel"), reqVO.getFaultLevel()));
            }

            if (reqVO.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), reqVO.getStatus()));
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

    private EquipmentFaultPrincipleSpecifications() {
    }
}
