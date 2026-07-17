package com.badminton.mes.module.scene.service;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.scene.controller.vo.SceneEffectiveParameterReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneParameterChangeLogRespVO;
import com.badminton.mes.module.scene.controller.vo.SceneProductionParameterPageReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneProductionParameterRespVO;
import com.badminton.mes.module.scene.controller.vo.SceneProductionParameterSaveReqVO;

/**
 * 生产参数用例接口。
 *
 * <p>由现场生产参数 Controller 调用；工序作业、报工等 Service 通过
 * {@link #getEffectiveParameter(SceneEffectiveParameterReqVO)} 读取指定工序在当前时间
 * 应生效的参数，启停和修改操作会写入变更日志。
 *
 * @author 刘涵
 * @date 2026/07/16
 */
public interface SceneProductionParameterService {

    /** 创建生产参数草稿。 */
    Long createParameter(SceneProductionParameterSaveReqVO reqVO);

    /** 修改生产参数定义。 */
    void updateParameter(Long id, SceneProductionParameterSaveReqVO reqVO);

    /** 启用参数，并记录启用原因和生效时间。 */
    void enableParameter(Long id, String reason);

    /** 停用参数，并记录停用原因和变更日志。 */
    void disableParameter(Long id, String reason);

    /** 查询单条生产参数详情。 */
    SceneProductionParameterRespVO getParameter(Long id);

    /** 分页查询生产参数定义。 */
    PageResult<SceneProductionParameterRespVO> getParameterPage(SceneProductionParameterPageReqVO reqVO);

    /** 按产品、工序和时间点解析当前应使用的有效参数。 */
    SceneProductionParameterRespVO getEffectiveParameter(SceneEffectiveParameterReqVO reqVO);

    /** 查询某条参数的启停和修改历史。 */
    List<SceneParameterChangeLogRespVO> getChangeLogs(Long id);
}
