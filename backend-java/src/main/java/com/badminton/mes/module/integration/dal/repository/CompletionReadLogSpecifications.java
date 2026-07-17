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
 * <p>固定排除逻辑删除日志，并按来源系统、完工单号和读取时间闭区间组合可选条件。来源系统与
 * 写日志时采用相同的大写规范化规则，避免查询因大小写差异漏掉同一消费方的记录。
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
            // 审计查询默认只展示当前有效日志。
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(reqVO.getSourceSystem())) {
                // 来源系统统一去空白并大写后精确匹配。
                predicates.add(criteriaBuilder.equal(root.get("sourceSystem"),
                        reqVO.getSourceSystem().trim().toUpperCase(Locale.ROOT)));
            }
            if (StringUtils.hasText(reqVO.getCompletionNo())) {
                predicates.add(criteriaBuilder.equal(root.get("completionNo"),
                        reqVO.getCompletionNo().trim()));
            }
            if (reqVO.getStartTime() != null) {
                // 起止时间均包含边界，字段使用实际 readTime。
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

    /** 纯静态查询条件构造器，不允许实例化。 */
    private CompletionReadLogSpecifications() {
    }
}
