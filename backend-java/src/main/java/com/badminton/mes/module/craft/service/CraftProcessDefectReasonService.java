package com.badminton.mes.module.craft.service;

import java.util.List;

import com.badminton.mes.module.craft.controller.vo.CraftProcessDefectReasonRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessDefectReasonSaveReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessDefectReasonUpdateReqVO;

/**
 * 工序不良原因关联 Service。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public interface CraftProcessDefectReasonService {

    /**
     * 为工序新增不良原因。
     *
     * @param processId 工序主键
     * @param reqVO     创建请求
     * @return 不良原因关联主键
     */
    Long createProcessDefectReason(Long processId, CraftProcessDefectReasonSaveReqVO reqVO);

    /**
     * 按客户端预期版本修改工序不良原因。
     *
     * @param processId 工序主键
     * @param reasonId  不良原因关联主键
     * @param reqVO     修改请求
     */
    void updateProcessDefectReason(Long processId, Long reasonId,
                                   CraftProcessDefectReasonUpdateReqVO reqVO);

    /**
     * 按客户端预期版本逻辑删除工序不良原因。
     *
     * @param processId       工序主键
     * @param reasonId        不良原因关联主键
     * @param expectedVersion 客户端读取时的版本号
     */
    void deleteProcessDefectReason(Long processId, Long reasonId, Integer expectedVersion);

    /**
     * 查询工序的未删除不良原因。
     *
     * @param processId 工序主键
     * @return 不良原因列表
     */
    List<CraftProcessDefectReasonRespVO> getProcessDefectReasons(Long processId);
}
