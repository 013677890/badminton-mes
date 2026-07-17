package com.badminton.mes.module.scene.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.scene.controller.vo.SceneProductionTaskPageReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneProductionTaskRespVO;
import com.badminton.mes.module.scene.controller.vo.SceneProductionTaskSaveReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneTaskProgressRespVO;

/**
 * 生产任务用例接口，由生产任务 Controller 调用，维护任务审核、下达、执行和关闭状态机。
 *
 * @author 刘涵
 * @date 2026/07/16
 */
public interface SceneProductionTaskService {

    /** 创建生产任务草稿。 */
    Long createTask(SceneProductionTaskSaveReqVO reqVO);

    /** 修改未进入执行阶段的生产任务。 */
    void updateTask(Long id, SceneProductionTaskSaveReqVO reqVO);

    /** 审核生产任务。 */
    void auditTask(Long id);

    /** 下达已审核生产任务，允许现场接单。 */
    void releaseTask(Long id);

    /** 开始执行生产任务。 */
    void startTask(Long id);

    /** 暂停生产任务并记录原因。 */
    void pauseTask(Long id, String reason);

    /** 恢复已暂停生产任务。 */
    void resumeTask(Long id);

    /** 关闭生产任务并记录关闭原因。 */
    void closeTask(Long id, String reason);

    /** 查询生产任务详情。 */
    SceneProductionTaskRespVO getTask(Long id);

    /** 分页查询生产任务。 */
    PageResult<SceneProductionTaskRespVO> getTaskPage(SceneProductionTaskPageReqVO reqVO);

    /** 查询生产任务当前完成进度。 */
    SceneTaskProgressRespVO getTaskProgress(Long id);
}
