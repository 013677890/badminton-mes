package com.badminton.mes.module.report.controller.vo;

import java.util.List;

import lombok.Data;

/**
 * 微信小程序实时看板聚合响应。
 *
 * @author Codex
 * @date 2026/07/15
 */
@Data
public class MiniAppRealtimeDashboardRespVO {

    /** 实时生产总览 */
    private RealtimeProductionRespVO.Overview overview;

    /** 当前在制任务 */
    private List<RealtimeProductionRespVO.Task> tasks = List.of();
}
