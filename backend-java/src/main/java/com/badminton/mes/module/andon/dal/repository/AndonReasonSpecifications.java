package com.badminton.mes.module.andon.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.andon.controller.vo.AndonReasonPageReqVO;
import com.badminton.mes.module.andon.dal.entity.AndonReasonEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 安灯异常原因分页动态查询条件。
 *
 * <p>关键词在原因编码和名称之间取 OR，其余类型、启停状态与逻辑删除条件共同取 AND。
 */
public final class AndonReasonSpecifications {

    /**
     * 根据分页请求构造原因筛选条件；未填写的可选条件不会限制结果集。
     *
     * @param request 原因分页筛选参数
     * @return 可与分页、排序组合执行的 JPA 查询条件
     */
    public static Specification<AndonReasonEntity> page(AndonReasonPageReqVO request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 原因管理列表固定排除逻辑删除数据。
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(request.getKeyword())) {
                // 转义 LIKE 通配符后执行字面量包含匹配，避免输入改变匹配范围。
                String pattern = "%" + escapeWildcards(request.getKeyword()) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("reasonCode"), pattern, '\\'),
                        criteriaBuilder.like(root.get("reasonName"), pattern, '\\')));
            }
            // 类型条件限定原因归属，防止跨安灯类型混合展示。
            if (request.getAndonTypeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("andonTypeId"), request.getAndonTypeId()));
            }
            // 启停状态采用精确匹配，不改变历史引用关系。
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

    private AndonReasonSpecifications() {
    }
}
