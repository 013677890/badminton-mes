package com.badminton.mes.module.scene.service;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.scene.controller.vo.*;
/** 生产任务用例接口。 @author 刘涵 */
public interface SceneProductionTaskService {
    Long createTask(SceneProductionTaskSaveReqVO reqVO);void updateTask(Long id,SceneProductionTaskSaveReqVO reqVO);
    void auditTask(Long id);void releaseTask(Long id);void startTask(Long id);void pauseTask(Long id,String reason);
    void resumeTask(Long id);void closeTask(Long id,String reason);SceneProductionTaskRespVO getTask(Long id);
    PageResult<SceneProductionTaskRespVO> getTaskPage(SceneProductionTaskPageReqVO reqVO);
    SceneTaskProgressRespVO getTaskProgress(Long id);
}
