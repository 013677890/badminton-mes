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
 * <p>规格固定限定审核通过且未逻辑删除的数据，再按审核时间闭区间、完工单号和工单号追加可选
 * 精确条件。外部读取接口因此无法通过请求参数访问待审核、驳回或已删除完工数据。
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
            // 审核通过与未删除是外部完工读取的强制安全边界，不由调用方选择。
            predicates.add(criteriaBuilder.equal(root.get("auditStatus"),
                    CompletionAuditStatusEnum.APPROVED.getStatus()));
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (reqVO.getStartTime() != null) {
                // 起始时间包含边界，按审核完成时间而非记录创建时间过滤。
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("auditTime"), reqVO.getStartTime()));
            }
            if (reqVO.getEndTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("auditTime"), reqVO.getEndTime()));
            }
            if (StringUtils.hasText(reqVO.getCompletionNo())) {
                // 完工单号是稳定业务键，去除首尾空白后精确匹配。
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

    /** 纯静态查询规格构造器，不允许实例化。 */
    private CompletionOrderSpecifications() {
    }
}
