package com.badminton.mes.module.report.service;

import java.util.List;
import java.util.Objects;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.report.constants.ReportErrorCodeConstants;
import org.springframework.stereotype.Component;

/**
 * 报表查询的数据范围解析器，请求条件只能缩小登录用户范围。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@Component
public class ReportDataScopeService {

    /**
     * 解析 SQL 查询应使用的车间、产线范围。
     *
     * @param requestedWorkshopId 请求车间
     * @param requestedLineId 请求产线
     * @return 服务端收敛后的范围
     */
    public ReportDataScope resolve(Long requestedWorkshopId, Long requestedLineId) {
        LoginUser user = SecurityContextHolder.getRequiredLoginUser();
        List<String> roles = user.getRoleCodes() == null ? List.of() : user.getRoleCodes();
        if (roles.contains(RoleCodeConstants.ADMIN)) {
            return new ReportDataScope(requestedWorkshopId, requestedLineId);
        }
        Long userWorkshopId = user.getWorkshopId();
        if (userWorkshopId == null || requestedWorkshopId != null
                && !Objects.equals(userWorkshopId, requestedWorkshopId)) {
            throw denied();
        }

        boolean workshopWide = roles.contains(RoleCodeConstants.WORKSHOP_MANAGER)
                || roles.contains(RoleCodeConstants.PMC) && user.getLineId() == null;
        if (workshopWide) {
            return new ReportDataScope(userWorkshopId, requestedLineId);
        }

        Long userLineId = user.getLineId();
        if (userLineId == null || requestedLineId != null && !Objects.equals(userLineId, requestedLineId)) {
            throw denied();
        }
        return new ReportDataScope(userWorkshopId, userLineId);
    }

    /**
     * 校验单个追溯对象是否属于当前用户范围。
     *
     * @param workshopId 对象车间
     * @param lineId 对象产线
     */
    public void checkObject(Long workshopId, Long lineId) {
        ReportDataScope scope = resolve(workshopId, lineId);
        if (scope.workshopId() != null && !Objects.equals(scope.workshopId(), workshopId)) {
            throw denied();
        }
        if (scope.lineId() != null && !Objects.equals(scope.lineId(), lineId)) {
            throw denied();
        }
    }

    private ServiceException denied() {
        return new ServiceException(ReportErrorCodeConstants.DATA_SCOPE_DENIED);
    }

    /** 服务端生效的数据范围。 */
    public record ReportDataScope(Long workshopId, Long lineId) {
    }
}
