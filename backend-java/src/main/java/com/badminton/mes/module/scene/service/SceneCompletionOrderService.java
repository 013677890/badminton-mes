package com.badminton.mes.module.scene.service;

import com.badminton.mes.module.scene.controller.vo.SceneCompletionAuditReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneCompletionCreateReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneCompletionSaveReqVO;

/**
 * 生产完工服务，由完工单 Controller 调用，并在审核或同步阶段被生产/集成流程引用。
 *
 * @author 刘涵
 * @date 2026/07/16
 */
public interface SceneCompletionOrderService {

    /** 创建生产完工单草稿。 */
    Long create(SceneCompletionCreateReqVO req);

    /** 修改尚未提交的完工单。 */
    void update(Long id, SceneCompletionSaveReqVO req);

    /** 提交完工单，锁定现场填报内容并进入审核流程。 */
    void submit(Long id);

    /** 审核完工单，记录审核意见和审核人。 */
    void audit(Long id, SceneCompletionAuditReqVO req);

    /** 将审核通过的完工单同步到下游生产/ERP 数据。 */
    void sync(Long id);
}
