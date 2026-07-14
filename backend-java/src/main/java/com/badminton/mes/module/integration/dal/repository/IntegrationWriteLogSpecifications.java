package com.badminton.mes.module.integration.dal.repository;

import com.badminton.mes.module.integration.controller.vo.IntegrationWriteLogPageReqVO;
import com.badminton.mes.module.integration.dal.entity.IntegrationWriteLogEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * 外部接口写入日志动态查询条件。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
public final class IntegrationWriteLogSpecifications {

    /**
     * 构造日志分页筛选条件。
     *
     * @param reqVO 查询参数
     * @return JPA Specification
     */
    public static Specification<IntegrationWriteLogEntity> page(
            IntegrationWriteLogPageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (StringUtils.hasText(reqVO.getInterfaceType())) {
                predicates.add(criteriaBuilder.equal(root.get("interfaceType"),
                        reqVO.getInterfaceType().trim().toUpperCase(java.util.Locale.ROOT)));
            }
            if (StringUtils.hasText(reqVO.getSourceSystem())) {
                predicates.add(criteriaBuilder.equal(root.get("sourceSystem"),
                        reqVO.getSourceSystem().trim().toUpperCase(java.util.Locale.ROOT)));
            }
            if (StringUtils.hasText(reqVO.getBusinessKey())) {
                predicates.add(criteriaBuilder.equal(root.get("businessKey"),
                        reqVO.getBusinessKey().trim()));
            }
            if (reqVO.getWriteStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("writeStatus"), reqVO.getWriteStatus()));
            }
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private IntegrationWriteLogSpecifications() {
    }
}
