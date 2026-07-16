package com.badminton.mes.module.scene.service;
import com.badminton.mes.module.scene.controller.vo.*;
/** 生产完工服务。 @author 刘涵 */
public interface SceneCompletionOrderService{
 Long create(SceneCompletionCreateReqVO req);
 void update(Long id,SceneCompletionSaveReqVO req);
 void submit(Long id);
 void audit(Long id,SceneCompletionAuditReqVO req);
 void sync(Long id);
}
