package com.badminton.mes.module.integration.dal.repository;

import com.badminton.mes.module.integration.controller.vo.ErpSyncLogPageReqVO;
import com.badminton.mes.module.integration.dal.entity.IntegrationWriteLogEntity;
import com.badminton.mes.module.integration.enums.IntegrationInterfaceTypeEnum;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * ERP 同步日志动态查询条件，固定过滤 interfaceType=ERP_TASK_SYNC。
 *
 * <p>该规格专供 ERP 任务同步日志页面，接口类型条件始终由代码写入，调用方只能继续按来源系统、
 * 业务键和写入状态缩小范围，避免误把其他集成接口日志混入 ERP 同步审计结果。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
public final class ErpSyncLogSpecifications {

    /**
     * 构造 ERP 任务同步日志分页筛选条件。
     *
     * @param reqVO 查询参数
     * @return JPA Specification
     */
    public static Specification<IntegrationWriteLogEntity> erpTaskSyncLogPage(
            ErpSyncLogPageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            // 固定限定 ERP_TASK_SYNC，防止复用日志表时跨接口串数据。
            predicates.add(criteriaBuilder.equal(root.get("interfaceType"),
                    IntegrationInterfaceTypeEnum.ERP_TASK_SYNC.getValue()));
            if (StringUtils.hasText(reqVO.getSourceSystem())) {
                // 来源系统与写入端统一大写，确保审计查询命中同一来源分区。
                predicates.add(criteriaBuilder.equal(root.get("sourceSystem"),
                        reqVO.getSourceSystem().trim().toUpperCase(Locale.ROOT)));
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
    private ErpSyncLogSpecifications() {
    }
}
