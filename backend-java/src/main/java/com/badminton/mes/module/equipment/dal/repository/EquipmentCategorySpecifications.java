package com.badminton.mes.module.equipment.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentCategoryPageReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentCategoryEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 设备类别动态查询条件。
 *
 * @author 角色C
 * @date 2026/07/09
 */
public final class EquipmentCategorySpecifications {

    /**
     * 构造分页筛选条件。
     *
     * @param reqVO 分页请求
     * @return JPA Specification
     */
    public static Specification<EquipmentCategoryEntity> page(EquipmentCategoryPageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (StringUtils.hasText(reqVO.getKeyword())) {
                String pattern = "%" + reqVO.getKeyword() + "%";
                Predicate codeLike = criteriaBuilder.like(root.get("categoryCode"), pattern);
                Predicate nameLike = criteriaBuilder.like(root.get("categoryName"), pattern);
                predicates.add(criteriaBuilder.or(codeLike, nameLike));
            }

            if (reqVO.getParentId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("parentId"), reqVO.getParentId()));
            }

            if (reqVO.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), reqVO.getStatus()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private EquipmentCategorySpecifications() {
    }
}
