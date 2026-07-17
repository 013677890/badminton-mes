package com.badminton.mes.module.equipment.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerPageReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentLedgerEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 设备台账分页查询的动态条件构造器。
 *
 * <p>所有正常列表固定过滤逻辑删除数据；关键字在设备编码、名称和型号间执行 OR 包含匹配，
 * 类别、制造商、运行状态、车间、产线及启停状态则按实际入参追加精确条件。不同条件组使用
 * AND 组合，使同一规格可覆盖资产检索、组织范围过滤和状态筛选等场景。
 *
 * @author 角色C
 * @date 2026/07/09
 */
public final class EquipmentLedgerSpecifications {

    /**
     * 构造设备台账分页筛选规格。
     *
     * <p>仅为非空请求项生成谓词，因而未填写的条件不会限制结果集。返回规格不负责排序或分页，
     * 这些行为由 Repository 执行时传入的分页对象决定。
     *
     * @param reqVO 分页请求
     * @return 可供台账 Repository 执行的组合查询条件
     */
    public static Specification<EquipmentLedgerEntity> page(EquipmentLedgerPageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 固定隔离逻辑删除台账，避免失效设备参与后续业务选择。
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (StringUtils.hasText(reqVO.getKeyword())) {
                // 关键字覆盖现场常用的编码、名称和型号三个识别维度。
                String escapedKeyword = escapeWildcards(reqVO.getKeyword());
                String pattern = "%" + escapedKeyword + "%";
                Predicate codeLike = criteriaBuilder.like(root.get("equipmentCode"), pattern, '\\');
                Predicate nameLike = criteriaBuilder.like(root.get("equipmentName"), pattern, '\\');
                Predicate modelLike = criteriaBuilder.like(root.get("equipmentModel"), pattern, '\\');
                predicates.add(criteriaBuilder.or(codeLike, nameLike, modelLike));
            }

            if (reqVO.getCategoryId() != null) {
                // 按设备类别主键精确过滤，不联表读取类别实体。
                predicates.add(criteriaBuilder.equal(root.get("categoryId"), reqVO.getCategoryId()));
            }

            if (reqVO.getManufacturerId() != null) {
                // 按制造商主键精确过滤，用于供应来源维度检索。
                predicates.add(criteriaBuilder.equal(root.get("manufacturerId"), reqVO.getManufacturerId()));
            }

            if (StringUtils.hasText(reqVO.getEquipmentStatus())) {
                // 运行状态使用枚举字符串精确匹配，避免跨状态混入。
                predicates.add(criteriaBuilder.equal(root.get("equipmentStatus"), reqVO.getEquipmentStatus()));
            }

            if (reqVO.getWorkshopId() != null) {
                // 限定所属车间，未传时不施加组织范围条件。
                predicates.add(criteriaBuilder.equal(root.get("workshopId"), reqVO.getWorkshopId()));
            }

            if (reqVO.getProductionLineId() != null) {
                // 限定所属产线，与车间条件同时存在时二者按 AND 组合。
                predicates.add(criteriaBuilder.equal(root.get("productionLineId"), reqVO.getProductionLineId()));
            }

            if (reqVO.getStatus() != null) {
                // 启停状态独立于设备运行状态，二者分别过滤不同业务维度。
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

    /** 纯静态条件构造器，不允许实例化。 */
    private EquipmentLedgerSpecifications() {
    }
}
