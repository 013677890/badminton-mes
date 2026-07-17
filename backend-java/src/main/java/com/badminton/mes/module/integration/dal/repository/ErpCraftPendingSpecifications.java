package com.badminton.mes.module.integration.dal.repository;

import java.util.ArrayList;
import java.util.Locale;

import com.badminton.mes.module.integration.controller.vo.ErpCraftPendingPageReqVO;
import com.badminton.mes.module.integration.dal.entity.ErpCraftPendingEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * ERP 工艺待确认分页条件。
 *
 * <p>按处理状态、来源系统和 ERP 路线编码拼装精确条件；不附加逻辑删除过滤是因为待确认表的
 * 状态列表需要完整反映同步和人工处理历史，是否展示由实体状态和调用场景共同决定。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
public final class ErpCraftPendingSpecifications {

    /** 构造 ERP 工艺暂存数据分页条件，分页和排序由调用方的 Pageable 决定。 */
    public static Specification<ErpCraftPendingEntity> page(ErpCraftPendingPageReqVO reqVO) {
        return (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
            if (reqVO.getStatus() != null) {
                // 状态精确匹配，用于分别查看待确认、失败、已确认或已驳回数据。
                predicates.add(builder.equal(root.get("status"), reqVO.getStatus()));
            }
            if (StringUtils.hasText(reqVO.getSourceSystem())) {
                // 来源系统沿用同步写入时的大写规范化规则。
                predicates.add(builder.equal(root.get("sourceSystem"),
                        reqVO.getSourceSystem().trim().toUpperCase(Locale.ROOT)));
            }
            if (StringUtils.hasText(reqVO.getErpRoutingCode())) {
                predicates.add(builder.equal(root.get("erpRoutingCode"),
                        reqVO.getErpRoutingCode().trim()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** 纯静态查询规格构造器，不允许实例化。 */
    private ErpCraftPendingSpecifications() {
    }
}
