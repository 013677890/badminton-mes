package com.badminton.mes.module.andon.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.andon.controller.vo.AndonConfigurationPageReqVO;
import com.badminton.mes.module.andon.dal.entity.AndonConfigurationEntity;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

/**
 * 安灯异常处理配置分页动态查询条件。
 *
 * <p>类型、业务产线、处理用户和启停状态均采用精确匹配，所有已提供条件与逻辑删除过滤条件取 AND。
 */
public final class AndonConfigurationSpecifications {

    /**
     * 根据分页请求构造配置筛选条件；空值字段表示不限制对应维度。
     *
     * @param request 配置分页筛选参数
     * @return 可与分页、排序组合执行的 JPA 查询条件
     */
    public static Specification<AndonConfigurationEntity> page(AndonConfigurationPageReqVO request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 配置管理列表只展示未逻辑删除的数据。
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            // 按安灯类型限定规则归属范围。
            if (request.getAndonTypeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("andonTypeId"), request.getAndonTypeId()));
            }
            // 此处筛选配置直接关联的业务产线，不以规范化 scopeLineId 替代。
            if (request.getProductionLineId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("productionLineId"), request.getProductionLineId()));
            }
            // 按明确处理用户筛选；角色责任主体不受此条件匹配。
            if (request.getHandlerUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("handlerUserId"), request.getHandlerUserId()));
            }
            // 启停状态精确过滤，未提供时同时保留不同状态的活动配置。
            if (request.getEnabledStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabledStatus"), request.getEnabledStatus()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AndonConfigurationSpecifications() {
    }
}
