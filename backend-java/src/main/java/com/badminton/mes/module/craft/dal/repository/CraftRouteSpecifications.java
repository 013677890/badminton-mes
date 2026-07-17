package com.badminton.mes.module.craft.dal.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.badminton.mes.module.craft.controller.vo.CraftRoutePageReqVO;
import com.badminton.mes.module.craft.dal.entity.CraftRouteEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 工艺路线动态分页查询条件。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public final class CraftRouteSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    /**
     * 构造路线分页查询条件。
     *
     * @param reqVO 分页查询请求
     * @return JPA Specification
     */
    public static Specification<CraftRouteEntity> page(CraftRoutePageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 路线列表只展示当前有效记录，历史软删除版本由审计链路负责追溯。
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (StringUtils.hasText(reqVO.getRoutingCode())) {
                // 用户输入先按路线编码规则转大写，再转义通配符并进行索引友好的前缀匹配。
                String codePrefix = escapeLike(reqVO.getRoutingCode().trim().toUpperCase(Locale.ROOT)) + "%";
                predicates.add(criteriaBuilder.like(root.get("routingCode"), codePrefix, LIKE_ESCAPE));
            }
            if (StringUtils.hasText(reqVO.getRoutingName())) {
                String namePrefix = escapeLike(reqVO.getRoutingName().trim()) + "%";
                predicates.add(criteriaBuilder.like(
                        root.get("routingName"), namePrefix, LIKE_ESCAPE));
            }
            if (StringUtils.hasText(reqVO.getRoutingVersion())) {
                predicates.add(criteriaBuilder.equal(
                        root.get("routingVersion"), reqVO.getRoutingVersion().trim().toUpperCase(Locale.ROOT)));
            }
            if (reqVO.getSourceType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("sourceType"), reqVO.getSourceType()));
            }
            if (reqVO.getRoutingStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("routingStatus"), reqVO.getRoutingStatus()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 转义 LIKE 查询中的通配符和转义字符，保证前缀筛选按字面值匹配。
     *
     * @param value 用户输入
     * @return 可安全用于 LIKE 模式的字面值
     */
    static String escapeLike(String value) {
        // 必须先转义反斜杠，再处理百分号和下划线，避免后续新增的转义符被重复解释。
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /** 工具类禁止实例化。 */
    private CraftRouteSpecifications() {
    }
}
