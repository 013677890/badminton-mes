package com.badminton.mes.module.report.service;

import java.util.List;

import com.badminton.mes.module.report.controller.vo.RealtimeProductionRespVO;
import com.badminton.mes.module.report.controller.vo.RealtimeReportQueryReqVO;

/**
 * 实时生产只读服务。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
public interface RealtimeProductionService {

    /** 查询实时生产总览。 */
    RealtimeProductionRespVO.Overview overview(RealtimeReportQueryReqVO reqVO);

    /**
     * 由系统看板任务查询实时生产总览，不依赖 HTTP 登录上下文。
     *
     * <p>仅供服务端受控的看板快照调用，调用方必须传入其允许发布的车间、产线范围。</p>
     */
    RealtimeProductionRespVO.Overview overviewForKanban(RealtimeReportQueryReqVO reqVO);

    /** 查询当前在制任务。 */
    List<RealtimeProductionRespVO.Task> tasks(RealtimeReportQueryReqVO reqVO);
}
