package com.badminton.mes.module.equipment.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerPageReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentManufacturerEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 设备制造商分页查询的动态条件构造器。
 *
 * <p>规格始终排除逻辑删除档案，并按请求选择性追加关键字和启停状态条件。关键字在制造商编码、
 * 名称之间执行 OR 包含匹配，状态与关键字组之间执行 AND，从而同时支持宽松检索和精确过滤。
 *
 * @author 角色C
 * @date 2026/07/09
 */
public final class EquipmentManufacturerSpecifications {

    /**
     * 构造设备制造商分页筛选规格。
     *
     * <p>请求项为空时不生成对应谓词；本方法只负责数据库过滤条件，不改变分页参数、排序规则
     * 或实体加载方式。
     *
     * @param reqVO 分页请求
     * @return 可供 Repository 分页执行的组合查询条件
     */
    public static Specification<EquipmentManufacturerEntity> page(EquipmentManufacturerPageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 制造商正常列表只允许读取尚未逻辑删除的主数据。
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (StringUtils.hasText(reqVO.getKeyword())) {
                // 编码和名称采用同一字面关键字，任一字段包含即可命中。
                String escapedKeyword = escapeWildcards(reqVO.getKeyword());
                String pattern = "%" + escapedKeyword + "%";
                Predicate codeLike = criteriaBuilder.like(root.get("manufacturerCode"), pattern);
                Predicate nameLike = criteriaBuilder.like(root.get("manufacturerName"), pattern);
                predicates.add(criteriaBuilder.or(codeLike, nameLike));
            }

            if (reqVO.getStatus() != null) {
                // 非空状态执行精确匹配；空值表示不区分启用与停用。
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
    private EquipmentManufacturerSpecifications() {
    }
}
