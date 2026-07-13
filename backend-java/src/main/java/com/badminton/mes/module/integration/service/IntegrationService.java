package com.badminton.mes.module.integration.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.integration.controller.vo.ExternalDispatchOrderWriteReqVO;
import com.badminton.mes.module.integration.controller.vo.ExternalWorkOrderWriteReqVO;
import com.badminton.mes.module.integration.controller.vo.IntegrationWriteLogPageReqVO;
import com.badminton.mes.module.integration.controller.vo.IntegrationWriteLogRespVO;
import com.badminton.mes.module.integration.controller.vo.IntegrationWriteResultRespVO;
import com.badminton.mes.module.integration.controller.vo.UnitWriteReqVO;

/**
 * 外部标准写入接口 Service。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
public interface IntegrationService {

    /**
     * 新增或更新计量单位，并记录写入结果。
     *
     * @param reqVO 单位写入请求
     * @return 写入结果
     */
    IntegrationWriteResultRespVO writeUnit(UnitWriteReqVO reqVO);

    /**
     * 按来源系统和外部工单号幂等写入生产工单。
     *
     * @param reqVO 外部工单写入请求
     * @return 写入结果
     */
    IntegrationWriteResultRespVO writeWorkOrder(ExternalWorkOrderWriteReqVO reqVO);

    /**
     * 按来源系统和外部任务单号幂等写入生产任务单（派工单）。
     *
     * @param reqVO 外部任务单写入请求
     * @return 写入结果
     */
    IntegrationWriteResultRespVO writeDispatchOrder(ExternalDispatchOrderWriteReqVO reqVO);

    /**
     * 分页查询外部接口写入结果。
     *
     * @param reqVO 分页筛选条件
     * @return 写入日志分页
     */
    PageResult<IntegrationWriteLogRespVO> getWriteLogPage(IntegrationWriteLogPageReqVO reqVO);
}
