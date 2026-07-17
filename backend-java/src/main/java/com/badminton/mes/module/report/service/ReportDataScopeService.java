package com.badminton.mes.module.report.service;

import com.badminton.mes.common.security.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 报表查询的数据范围解析器。
 *
 * <p>微信小程序和报表查询对所有已登录用户开放，车间、产线仅作为查询筛选条件，
 * 不再依据职位或用户所属组织收敛数据范围。
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
     * @return 本次查询使用的筛选范围
     */
    public ReportDataScope resolve(Long requestedWorkshopId, Long requestedLineId) {
        SecurityContextHolder.getRequiredLoginUser();
        return new ReportDataScope(requestedWorkshopId, requestedLineId);
    }

    /**
     * 确认当前追溯查询存在有效登录上下文。
     *
     * @param workshopId 对象车间
     * @param lineId 对象产线
     */
    public void checkObject(Long workshopId, Long lineId) {
        resolve(workshopId, lineId);
    }

    /** 服务端生效的数据范围。 */
    public record ReportDataScope(Long workshopId, Long lineId) {
    }
}
