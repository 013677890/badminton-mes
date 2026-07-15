package com.badminton.mes.module.scene.service;

import com.badminton.mes.module.scene.controller.vo.*;

/** 返修工单服务。 @author 刘涵 */
public interface SceneRepairWorkOrderService {
    SceneRepairRespVO create(SceneRepairCreateReqVO request);
    void assign(Long id, Long assigneeId);
    void start(Long id);
    void addRecord(Long id, SceneRepairRecordCreateReqVO request);
    void recheck(Long id, SceneRepairRecheckReqVO request);
    void close(Long id);
    SceneRepairRespVO get(Long id);
}
