package com.badminton.mes.module.equipment.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentFaultPrinciplePageReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentFaultPrincipleEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 设备故障原理分页查询的动态条件构造器。
 *
 * <p>规格固定排除逻辑删除字典项；关键字同时覆盖故障编码、名称和描述，类别、故障等级及启停状态
 * 按非空入参精确匹配。关键字字段内部使用 OR，其余条件与关键字组使用 AND，便于按设备类别
 * 缩小可选故障原理范围。
 *
 * @author 角色C
 * @date 2026/07/10
 */
public final class EquipmentFaultPrincipleSpecifications {

    /**
     * 构造故障原理分页筛选规格。
     *
     * <p>每个可选条件仅在请求提供有效值时加入，返回规格只约束结果集，不负责分页和排序。
     *
     * @param reqVO 分页请求
     * @return 可供故障原理 Repository 执行的组合查询条件
     */
    public static Specification<EquipmentFaultPrincipleEntity> page(EquipmentFaultPrinciplePageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 正常字典查询必须隔离逻辑删除项，防止其再次被报修业务选择。
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (StringUtils.hasText(reqVO.getKeyword())) {
                // 编码、名称或描述任一字段包含关键字即可命中。
                String escapedKeyword = escapeWildcards(reqVO.getKeyword());
                String pattern = "%" + escapedKeyword + "%";
                Predicate codeLike = criteriaBuilder.like(root.get("faultCode"), pattern, '\\');
                Predicate nameLike = criteriaBuilder.like(root.get("faultName"), pattern, '\\');
                Predicate descriptionLike = criteriaBuilder.like(root.get("faultDescription"), pattern, '\\');
                predicates.add(criteriaBuilder.or(codeLike, nameLike, descriptionLike));
            }

            if (reqVO.getCategoryId() != null) {
                // 类别条件只匹配明确归属该类别的数据，不自动并入 categoryId 为空的通用项。
                predicates.add(criteriaBuilder.equal(root.get("categoryId"), reqVO.getCategoryId()));
            }

            if (StringUtils.hasText(reqVO.getFaultLevel())) {
                // 故障等级按枚举字符串精确过滤。
                predicates.add(criteriaBuilder.equal(root.get("faultLevel"), reqVO.getFaultLevel()));
            }

            if (reqVO.getStatus() != null) {
                // 非空状态区分启用与停用字典项。
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
    private EquipmentFaultPrincipleSpecifications() {
    }
}
