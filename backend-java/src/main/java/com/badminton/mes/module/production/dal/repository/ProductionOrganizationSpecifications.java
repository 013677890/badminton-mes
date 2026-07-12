package com.badminton.mes.module.production.dal.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.badminton.mes.module.production.controller.vo.ProductionLinePageReqVO;
import com.badminton.mes.module.production.controller.vo.WorkshopPageReqVO;
import com.badminton.mes.module.production.dal.entity.ProductionLineEntity;
import com.badminton.mes.module.production.dal.entity.WorkshopEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * 车间与产线动态分页条件。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
public final class ProductionOrganizationSpecifications {

    /**
     * 构造车间分页条件。
     *
     * @param reqVO 分页请求
     * @return JPA 查询条件
     */
    public static Specification<WorkshopEntity> workshopPage(WorkshopPageReqVO reqVO) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(reqVO.getWorkshopCode())) {
                String prefix = escapeLike(
                        reqVO.getWorkshopCode().trim().toUpperCase(Locale.ROOT)) + "%";
                predicates.add(builder.like(root.get("workshopCode"), prefix, '\\'));
            }
            if (StringUtils.hasText(reqVO.getWorkshopName())) {
                String prefix = escapeLike(reqVO.getWorkshopName().trim()) + "%";
                predicates.add(builder.like(root.get("workshopName"), prefix, '\\'));
            }
            addEqual(predicates, builder, root.get("managerId"), reqVO.getManagerId());
            addEqual(predicates, builder, root.get("status"), reqVO.getStatus());
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 构造产线分页条件。
     *
     * @param reqVO 分页请求
     * @return JPA 查询条件
     */
    public static Specification<ProductionLineEntity> productionLinePage(
            ProductionLinePageReqVO reqVO) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(reqVO.getLineCode())) {
                String prefix = escapeLike(
                        reqVO.getLineCode().trim().toUpperCase(Locale.ROOT)) + "%";
                predicates.add(builder.like(root.get("lineCode"), prefix, '\\'));
            }
            if (StringUtils.hasText(reqVO.getLineName())) {
                String prefix = escapeLike(reqVO.getLineName().trim()) + "%";
                predicates.add(builder.like(root.get("lineName"), prefix, '\\'));
            }
            addEqual(predicates, builder, root.get("workshopId"), reqVO.getWorkshopId());
            addEqual(predicates, builder, root.get("status"), reqVO.getStatus());
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** 添加非空等值条件。 */
    private static void addEqual(List<Predicate> predicates, CriteriaBuilder builder,
                                 Path<?> path, Object value) {
        if (value != null) {
            predicates.add(builder.equal(path, value));
        }
    }

    /** 转义 LIKE 通配符与转义符。 */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private ProductionOrganizationSpecifications() {
    }
}
