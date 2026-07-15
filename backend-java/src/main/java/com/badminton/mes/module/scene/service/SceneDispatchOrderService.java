package com.badminton.mes.module.scene.service;
import java.util.List;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.scene.controller.vo.*;
/** 工序派工用例接口。 @author 刘涵 */
public interface SceneDispatchOrderService {
    Long generate(SceneDispatchGenerateReqVO reqVO);void confirm(Long id);void cancel(Long id);
    SceneDispatchOrderRespVO get(Long id);PageResult<SceneDispatchOrderRespVO> page(SceneDispatchPageReqVO reqVO);
    List<SceneDispatchDetailRespVO> operations(Long id);
}
