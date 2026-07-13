package com.badminton.mes.module.scene.service;

import java.util.List;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.scene.controller.vo.*;

/** 生产参数用例接口。 @author 刘涵 */
public interface SceneProductionParameterService {
    Long createParameter(SceneProductionParameterSaveReqVO reqVO);
    void updateParameter(Long id, SceneProductionParameterSaveReqVO reqVO);
    void enableParameter(Long id, String reason);
    void disableParameter(Long id, String reason);
    SceneProductionParameterRespVO getParameter(Long id);
    PageResult<SceneProductionParameterRespVO> getParameterPage(SceneProductionParameterPageReqVO reqVO);
    SceneProductionParameterRespVO getEffectiveParameter(SceneEffectiveParameterReqVO reqVO);
    List<SceneParameterChangeLogRespVO> getChangeLogs(Long id);
}
