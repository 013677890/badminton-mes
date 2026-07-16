package com.badminton.mes.module.report.service.impl;

import java.time.Duration;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.report.constants.ReportErrorCodeConstants;
import com.badminton.mes.module.report.controller.vo.ReportQueryReqVO;
import com.badminton.mes.module.report.service.ReportDataScopeService;
import com.badminton.mes.module.report.service.ReportDataScopeService.ReportDataScope;
import com.badminton.mes.module.report.service.dto.ReportQueryCriteria;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * M4 查询范围校验和服务端数据权限条件组装。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@Component
public class ReportQuerySupport {

    public static final int EXPORT_MAX_ROWS = 10_000;
    public static final int DEFECT_MAX_ROWS = 10_000;
    private static final Duration QUERY_MAX_RANGE = Duration.ofDays(366);
    private static final Duration EXPORT_MAX_RANGE = Duration.ofDays(31);

    private final ReportDataScopeService dataScopeService;

    public ReportQuerySupport(ReportDataScopeService dataScopeService) {
        this.dataScopeService = dataScopeService;
    }

    /** 校验普通查询并返回已收敛数据范围的条件。 */
    public ReportQueryCriteria criteria(ReportQueryReqVO reqVO) {
        Duration duration = validateRange(reqVO);
        if (duration.compareTo(QUERY_MAX_RANGE) > 0) {
            throw new ServiceException(ReportErrorCodeConstants.QUERY_RANGE_EXCEEDED);
        }
        ReportDataScope scope = dataScopeService.resolve(reqVO.getWorkshopId(), reqVO.getLineId());
        return new ReportQueryCriteria(reqVO.getStartTime(), reqVO.getEndTime(),
                scope.workshopId(), scope.lineId(), reqVO.getProductId(), reqVO.getWorkOrderId(),
                reqVO.getTaskId(), reqVO.getProcessId(), reqVO.getShiftId(),
                StringUtils.hasText(reqVO.getBatchNo()) ? reqVO.getBatchNo().trim() : null,
                reqVO.getStatus());
    }

    /** 校验同步导出最多 31 天。 */
    public ReportQueryCriteria exportCriteria(ReportQueryReqVO reqVO) {
        Duration duration = validateRange(reqVO);
        if (duration.compareTo(EXPORT_MAX_RANGE) > 0) {
            throw new ServiceException(ReportErrorCodeConstants.EXPORT_RANGE_EXCEEDED);
        }
        return criteria(reqVO);
    }

    private Duration validateRange(ReportQueryReqVO reqVO) {
        if (reqVO.getStartTime() == null || reqVO.getEndTime() == null
                || reqVO.getEndTime().isBefore(reqVO.getStartTime())) {
            throw new ServiceException(ReportErrorCodeConstants.QUERY_RANGE_INVALID);
        }
        return Duration.between(reqVO.getStartTime(), reqVO.getEndTime());
    }
}
