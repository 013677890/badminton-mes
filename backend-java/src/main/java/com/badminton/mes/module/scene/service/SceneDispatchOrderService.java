package com.badminton.mes.module.scene.service;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.scene.controller.vo.SceneDispatchDetailRespVO;
import com.badminton.mes.module.scene.controller.vo.SceneDispatchGenerateReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneDispatchOrderRespVO;
import com.badminton.mes.module.scene.controller.vo.SceneDispatchPageReqVO;

/**
 * 工序派工用例接口，由派工单 Controller 调用；生成派工时读取生产任务和工艺路线，
 * 工序作业 Service 再读取本接口产生的派工明细。
 *
 * @author 刘涵
 * @date 2026/07/16
 */
public interface SceneDispatchOrderService {

    /** 根据生产任务生成派工单及其工序明细。 */
    Long generate(SceneDispatchGenerateReqVO reqVO);

    /** 确认派工单，使其进入现场可执行状态。 */
    void confirm(Long id);

    /** 取消尚未执行的派工单。 */
    void cancel(Long id);

    /** 查询派工单详情。 */
    SceneDispatchOrderRespVO get(Long id);

    /** 分页查询派工单。 */
    PageResult<SceneDispatchOrderRespVO> page(SceneDispatchPageReqVO reqVO);

    /** 查询派工单包含的工序明细。 */
    List<SceneDispatchDetailRespVO> operations(Long id);
}
