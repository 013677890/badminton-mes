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
 * @author 张竹灏
 * @date 2026/07/13
 */
public final class ErpCraftPendingSpecifications {

    public static Specification<ErpCraftPendingEntity> page(ErpCraftPendingPageReqVO reqVO) {
        return (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
            if (reqVO.getStatus() != null) {
                predicates.add(builder.equal(root.get("status"), reqVO.getStatus()));
            }
            if (StringUtils.hasText(reqVO.getSourceSystem())) {
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

    private ErpCraftPendingSpecifications() {
    }
}
