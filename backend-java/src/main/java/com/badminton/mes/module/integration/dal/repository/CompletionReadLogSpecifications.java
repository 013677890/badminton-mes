package com.badminton.mes.module.integration.dal.repository;

import java.util.ArrayList;
import java.util.Locale;

import com.badminton.mes.module.integration.controller.vo.CompletionReadLogPageReqVO;
import com.badminton.mes.module.integration.dal.entity.CompletionReadLogEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 生产完工单读取日志动态查询条件。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
public final class CompletionReadLogSpecifications {

    /**
     * 构造读取日志分页条件。
     *
     * @param reqVO 查询参数
     * @return JPA Specification
     */
    public static Specification<CompletionReadLogEntity> page(
            CompletionReadLogPageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(reqVO.getSourceSystem())) {
                predicates.add(criteriaBuilder.equal(root.get("sourceSystem"),
                        reqVO.getSourceSystem().trim().toUpperCase(Locale.ROOT)));
            }
            if (StringUtils.hasText(reqVO.getCompletionNo())) {
                predicates.add(criteriaBuilder.equal(root.get("completionNo"),
                        reqVO.getCompletionNo().trim()));
            }
            if (reqVO.getStartTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("readTime"), reqVO.getStartTime()));
            }
            if (reqVO.getEndTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("readTime"), reqVO.getEndTime()));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private CompletionReadLogSpecifications() {
    }
}
