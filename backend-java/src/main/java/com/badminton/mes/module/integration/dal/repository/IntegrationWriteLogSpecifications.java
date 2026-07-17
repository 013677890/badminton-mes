package com.badminton.mes.module.integration.dal.repository;

import com.badminton.mes.module.integration.controller.vo.IntegrationWriteLogPageReqVO;
import com.badminton.mes.module.integration.dal.entity.IntegrationWriteLogEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * 外部接口写入日志动态查询条件。
 *
 * <p>所有请求条件均为可选精确匹配；接口类型和来源系统按写入端的统一大写协议规范化，业务键
 * 保留其原始大小写语义，仅去除首尾空白。该类只负责 WHERE 条件，不决定分页和排序。
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
                // 接口类型是日志业务分区，按大写枚举值精确过滤。
                predicates.add(criteriaBuilder.equal(root.get("interfaceType"),
                        reqVO.getInterfaceType().trim().toUpperCase(java.util.Locale.ROOT)));
            }
            if (StringUtils.hasText(reqVO.getSourceSystem())) {
                // 来源系统采用与写入命令一致的 Locale.ROOT 大写规则。
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

    /** 纯静态查询规格构造器，不允许实例化。 */
    private IntegrationWriteLogSpecifications() {
    }
}
