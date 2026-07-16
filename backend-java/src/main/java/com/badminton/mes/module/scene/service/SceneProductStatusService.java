package com.badminton.mes.module.scene.service;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.scene.controller.vo.SceneProcessHistoryRespVO;
import com.badminton.mes.module.scene.controller.vo.SceneProductStatusPageReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneProductStatusRespVO;
import com.badminton.mes.module.scene.controller.vo.SceneStatusHistoryRespVO;

/**
 * 产品状态查询用例，由产品追溯 Controller 调用，聚合批次状态、状态历史和工序历史。
 *
 * @author 刘涵
 * @date 2026/07/16
 */
public interface SceneProductStatusService {

    /** 按批次号查询产品当前状态。 */
    SceneProductStatusRespVO getByBatch(String batchNo);

    /** 分页查询产品状态。 */
    PageResult<SceneProductStatusRespVO> page(SceneProductStatusPageReqVO req);

    /** 查询产品状态变更历史。 */
    List<SceneStatusHistoryRespVO> histories(Long id);

    /** 查询产品经过的工序历史。 */
    List<SceneProcessHistoryRespVO> processHistories(Long id);
}
