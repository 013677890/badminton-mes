package com.badminton.mes.module.equipment.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerPageReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentLedgerEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 设备台账动态查询条件。
 *
 * @author 角色C
 * @date 2026/07/09
 */
public final class EquipmentLedgerSpecifications {

    /**
     * 构造分页筛选条件。
     *
     * @param reqVO 分页请求
     * @return JPA Specification
     */
    public static Specification<EquipmentLedgerEntity> page(EquipmentLedgerPageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (StringUtils.hasText(reqVO.getKeyword())) {
                String escapedKeyword = escapeWildcards(reqVO.getKeyword());
                String pattern = "%" + escapedKeyword + "%";
                Predicate codeLike = criteriaBuilder.like(root.get("equipmentCode"), pattern, '\\');
                Predicate nameLike = criteriaBuilder.like(root.get("equipmentName"), pattern, '\\');
                Predicate modelLike = criteriaBuilder.like(root.get("equipmentModel"), pattern, '\\');
                predicates.add(criteriaBuilder.or(codeLike, nameLike, modelLike));
            }

            if (reqVO.getCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("categoryId"), reqVO.getCategoryId()));
            }

            if (reqVO.getManufacturerId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("manufacturerId"), reqVO.getManufacturerId()));
            }

            if (StringUtils.hasText(reqVO.getEquipmentStatus())) {
                predicates.add(criteriaBuilder.equal(root.get("equipmentStatus"), reqVO.getEquipmentStatus()));
            }

            if (reqVO.getWorkshopId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("workshopId"), reqVO.getWorkshopId()));
            }

            if (reqVO.getProductionLineId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("productionLineId"), reqVO.getProductionLineId()));
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

    private EquipmentLedgerSpecifications() {
    }
}
