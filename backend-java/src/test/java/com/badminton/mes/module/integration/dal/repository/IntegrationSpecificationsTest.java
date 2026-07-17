package com.badminton.mes.module.integration.dal.repository;

import java.time.LocalDateTime;

import com.badminton.mes.module.integration.controller.vo.CompletionOrderPageReqVO;
import com.badminton.mes.module.integration.controller.vo.CompletionReadLogPageReqVO;
import com.badminton.mes.module.integration.controller.vo.DeviceCountExceptionPageReqVO;
import com.badminton.mes.module.integration.dal.entity.CompletionOrderEntity;
import com.badminton.mes.module.integration.dal.entity.CompletionReadLogEntity;
import com.badminton.mes.module.integration.dal.entity.IntegrationDeviceCountExceptionEntity;
import com.badminton.mes.module.integration.enums.CompletionAuditStatusEnum;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 集成接口动态查询条件与闭区间边界测试。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
class IntegrationSpecificationsTest {

    @Test
    @DisplayName("完工单查询：始终限定已审核未删除并应用全部筛选和闭区间")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void completionOrderSpecificationAppliesApprovedFiltersAndClosedRange() {
        CompletionOrderPageReqVO reqVO = new CompletionOrderPageReqVO();
        LocalDateTime startTime = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 7, 13, 23, 59);
        reqVO.setStartTime(startTime);
        reqVO.setEndTime(endTime);
        reqVO.setCompletionNo("  CO-001  ");
        reqVO.setWorkOrderNo("  WO-001  ");
        Root<CompletionOrderEntity> root = mock(Root.class, RETURNS_DEEP_STUBS);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);

        Specification<CompletionOrderEntity> specification =
                CompletionOrderSpecifications.approvedPage(reqVO);
        specification.toPredicate(root, query, builder);

        verify(builder).equal(root.get("auditStatus"),
                CompletionAuditStatusEnum.APPROVED.getStatus());
        verify(builder).isFalse(root.get("deleted"));
        verify(builder).greaterThanOrEqualTo(
                any(Expression.class), eq(startTime));
        verify(builder).lessThanOrEqualTo(
                any(Expression.class), eq(endTime));
        verify(builder).equal(root.get("completionNo"), "CO-001");
        verify(builder).equal(root.get("workOrderNo"), "WO-001");
    }

    @Test
    @DisplayName("设备计数异常查询：编码归一化并应用状态与闭区间")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void deviceExceptionSpecificationNormalizesCodesAndAppliesRange() {
        DeviceCountExceptionPageReqVO reqVO = new DeviceCountExceptionPageReqVO();
        LocalDateTime startTime = LocalDateTime.of(2026, 7, 2, 8, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 7, 12, 18, 0);
        reqVO.setSourceSystem(" device-gateway ");
        reqVO.setEquipmentCode(" eqp-01 ");
        reqVO.setExceptionType(" count_rollback ");
        reqVO.setHandleStatus(0);
        reqVO.setStartTime(startTime);
        reqVO.setEndTime(endTime);
        Root<IntegrationDeviceCountExceptionEntity> root = mock(Root.class, RETURNS_DEEP_STUBS);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);

        DeviceCountExceptionSpecifications.page(reqVO)
                .toPredicate(root, mock(CriteriaQuery.class), builder);

        verify(builder).isFalse(root.get("deleted"));
        verify(builder).equal(root.get("sourceSystem"), "DEVICE-GATEWAY");
        verify(builder).equal(root.get("equipmentCode"), "EQP-01");
        verify(builder).equal(root.get("exceptionType"), "COUNT_ROLLBACK");
        verify(builder).equal(root.get("handleStatus"), 0);
        verify(builder).greaterThanOrEqualTo(any(Expression.class), eq(startTime));
        verify(builder).lessThanOrEqualTo(any(Expression.class), eq(endTime));
    }

    @Test
    @DisplayName("完工读取日志查询：来源归一化并应用单号与闭区间")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void completionReadLogSpecificationNormalizesSourceAndAppliesRange() {
        CompletionReadLogPageReqVO reqVO = new CompletionReadLogPageReqVO();
        LocalDateTime startTime = LocalDateTime.of(2026, 7, 3, 8, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 7, 11, 18, 0);
        reqVO.setSourceSystem(" wms-main ");
        reqVO.setCompletionNo(" CO-002 ");
        reqVO.setStartTime(startTime);
        reqVO.setEndTime(endTime);
        Root<CompletionReadLogEntity> root = mock(Root.class, RETURNS_DEEP_STUBS);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);

        CompletionReadLogSpecifications.page(reqVO)
                .toPredicate(root, mock(CriteriaQuery.class), builder);

        verify(builder).isFalse(root.get("deleted"));
        verify(builder).equal(root.get("sourceSystem"), "WMS-MAIN");
        verify(builder).equal(root.get("completionNo"), "CO-002");
        verify(builder).greaterThanOrEqualTo(any(Expression.class), eq(startTime));
        verify(builder).lessThanOrEqualTo(any(Expression.class), eq(endTime));
    }
}
