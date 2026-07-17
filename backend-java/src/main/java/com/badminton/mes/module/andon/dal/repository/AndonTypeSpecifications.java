package com.badminton.mes.module.andon.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.andon.controller.vo.AndonTypePageReqVO;
import com.badminton.mes.module.andon.dal.entity.AndonTypeEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 安灯类型分页动态查询条件。
 *
 * <p>关键词在类型编码和名称之间取 OR，异常类别、处理模式、启停状态与逻辑删除条件共同取 AND。
 */
public final class AndonTypeSpecifications {

    /**
     * 根据分页请求构造类型筛选条件；未填写的可选条件不会限制结果集。
     *
     * @param request 类型分页筛选参数
     * @return 可与分页、排序组合执行的 JPA 查询条件
     */
    public static Specification<AndonTypeEntity> page(AndonTypePageReqVO request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 类型管理列表固定排除逻辑删除数据。
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(request.getKeyword())) {
                // 转义 LIKE 通配符后，在类型编码和名称中执行字面量包含匹配。
                String pattern = "%" + escapeWildcards(request.getKeyword()) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("typeCode"), pattern, '\\'),
                        criteriaBuilder.like(root.get("typeName"), pattern, '\\')));
            }
            // 异常类别和处理模式均按业务枚举值精确匹配。
            if (StringUtils.hasText(request.getExceptionCategory())) {
                predicates.add(criteriaBuilder.equal(root.get("exceptionCategory"), request.getExceptionCategory()));
            }
            if (StringUtils.hasText(request.getHandlingMode())) {
                predicates.add(criteriaBuilder.equal(root.get("handlingMode"), request.getHandlingMode()));
            }
            // 启停状态未提供时同时返回不同状态下的未删除类型。
            if (request.getEnabledStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabledStatus"), request.getEnabledStatus()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** 将 LIKE 特殊字符转义为普通字符，配合查询中显式指定的反斜杠转义符使用。 */
    private static String escapeWildcards(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private AndonTypeSpecifications() {
    }
}
