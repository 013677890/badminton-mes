package com.badminton.mes.module.scene.service;
import java.util.List;import com.badminton.mes.common.core.PageResult;import com.badminton.mes.module.scene.controller.vo.*;
/** 工序作业用例接口。 @author 刘涵 */
public interface SceneOperationJobService {
 PageResult<SceneDispatchDetailRespVO> page(SceneOperationJobPageReqVO req);List<SceneDispatchDetailRespVO> my();
 SceneDispatchDetailRespVO get(Long id);void scan(Long id,SceneOperationScanReqVO req);void start(Long id);
 void pause(Long id,String reason);void finish(Long id);
}
