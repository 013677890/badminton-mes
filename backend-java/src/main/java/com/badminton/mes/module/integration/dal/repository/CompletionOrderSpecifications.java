package com.badminton.mes.module.integration.dal.repository;

import java.util.ArrayList;

import com.badminton.mes.module.integration.controller.vo.CompletionOrderPageReqVO;
import com.badminton.mes.module.integration.dal.entity.CompletionOrderEntity;
import com.badminton.mes.module.integration.enums.CompletionAuditStatusEnum;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 已审核生产完工单读取条件。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
public final class CompletionOrderSpecifications {

    /**
     * 构造只包含已审核且未删除完工单的分页条件。
     *
     * @param reqVO 查询参数
     * @return JPA Specification
     */
    public static Specification<CompletionOrderEntity> approvedPage(
            CompletionOrderPageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(criteriaBuilder.equal(root.get("auditStatus"),
                    CompletionAuditStatusEnum.APPROVED.getStatus()));
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (reqVO.getStartTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("auditTime"), reqVO.getStartTime()));
            }
            if (reqVO.getEndTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("auditTime"), reqVO.getEndTime()));
            }
            if (StringUtils.hasText(reqVO.getCompletionNo())) {
                predicates.add(criteriaBuilder.equal(root.get("completionNo"),
                        reqVO.getCompletionNo().trim()));
            }
            if (StringUtils.hasText(reqVO.getWorkOrderNo())) {
                predicates.add(criteriaBuilder.equal(root.get("workOrderNo"),
                        reqVO.getWorkOrderNo().trim()));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private CompletionOrderSpecifications() {
    }
}
