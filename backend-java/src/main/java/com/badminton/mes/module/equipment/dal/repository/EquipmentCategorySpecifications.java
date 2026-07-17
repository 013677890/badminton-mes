package com.badminton.mes.module.equipment.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentCategoryPageReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentCategoryEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 设备类别分页查询的动态条件构造器。
 *
 * <p>将请求中实际传入的关键字、父类别和启停状态依次转换为 Criteria 谓词；各可选条件之间
 * 使用 AND 连接，关键字内部对类别编码和类别名称使用 OR 包含匹配。无论请求是否携带筛选项，
 * 都固定追加 {@code deleted = false}，避免逻辑删除类别进入树形列表和选择数据源。
 *
 * @author 角色C
 * @date 2026/07/09
 */
public final class EquipmentCategorySpecifications {

    /**
     * 构造设备类别分页筛选规格。
     *
     * <p>关键字为空时不生成模糊条件；父类别和状态非空时分别精确匹配。返回的规格只描述
     * WHERE 条件，分页和排序仍由调用方传入的 {@code Pageable} 控制。
     *
     * @param reqVO 分页请求
     * @return 可交由 {@link org.springframework.data.jpa.repository.JpaSpecificationExecutor} 执行的组合查询条件
     */
    public static Specification<EquipmentCategoryEntity> page(EquipmentCategoryPageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 逻辑删除过滤是所有正常类别查询的固定边界，不交由调用方选择。
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (StringUtils.hasText(reqVO.getKeyword())) {
                // 编码或名称任一字段包含关键字即可命中，转义后的文本不应主动扩大匹配范围。
                String escapedKeyword = escapeWildcards(reqVO.getKeyword());
                String pattern = "%" + escapedKeyword + "%";
                Predicate codeLike = criteriaBuilder.like(root.get("categoryCode"), pattern);
                Predicate nameLike = criteriaBuilder.like(root.get("categoryName"), pattern);
                predicates.add(criteriaBuilder.or(codeLike, nameLike));
            }

            if (reqVO.getParentId() != null) {
                // 精确限定直接父节点；该条件不递归包含更深层后代。
                predicates.add(criteriaBuilder.equal(root.get("parentId"), reqVO.getParentId()));
            }

            if (reqVO.getStatus() != null) {
                // 状态条件区分启用和停用数据，未传时同时返回两类有效记录。
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
    private EquipmentCategorySpecifications() {
    }
}
