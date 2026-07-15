package com.badminton.mes.module.scene.service;
import java.util.List;import com.badminton.mes.common.core.PageResult;import com.badminton.mes.module.scene.controller.vo.*;
/** 产品状态查询用例。 @author 刘涵 */
public interface SceneProductStatusService {
 SceneProductStatusRespVO getByBatch(String batchNo);PageResult<SceneProductStatusRespVO> page(SceneProductStatusPageReqVO req);
 List<SceneStatusHistoryRespVO> histories(Long id);List<SceneProcessHistoryRespVO> processHistories(Long id);
}
