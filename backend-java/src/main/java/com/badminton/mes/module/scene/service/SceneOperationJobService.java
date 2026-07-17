package com.badminton.mes.module.scene.service;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.scene.controller.vo.SceneDispatchDetailRespVO;
import com.badminton.mes.module.scene.controller.vo.SceneOperationJobPageReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneOperationScanReqVO;

/**
 * 工序作业用例接口，由工序作业 Controller 调用；扫码、开始、暂停和完工会校验派工、
 * 当前操作人和生产参数，并把结果交给报工服务。
 *
 * @author 刘涵
 * @date 2026/07/16
 */
public interface SceneOperationJobService {

    /** 分页查询当前用户可处理的工序作业。 */
    PageResult<SceneDispatchDetailRespVO> page(SceneOperationJobPageReqVO req);

    /** 查询当前登录用户的待处理工序作业。 */
    List<SceneDispatchDetailRespVO> my();

    /** 查询单条派工工序详情。 */
    SceneDispatchDetailRespVO get(Long id);

    /** 扫描工序条码，校验工序与批次后建立作业上下文。 */
    void scan(Long id, SceneOperationScanReqVO req);

    /** 开始工序作业。 */
    void start(Long id);

    /** 暂停工序作业并记录原因。 */
    void pause(Long id, String reason);

    /** 完成工序作业并生成相应报工事实。 */
    void finish(Long id);
}
