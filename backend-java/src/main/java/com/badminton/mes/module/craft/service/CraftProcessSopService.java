package com.badminton.mes.module.craft.service;

import java.util.List;

import com.badminton.mes.module.craft.controller.vo.CraftProcessSopRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessSopSaveReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessSopUpdateReqVO;

/**
 * 工序 SOP 关联 Service。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public interface CraftProcessSopService {

    /**
     * 为工序新增 SOP。
     *
     * @param processId 工序主键
     * @param reqVO     创建请求
     * @return SOP 关联主键
     */
    Long createProcessSop(Long processId, CraftProcessSopSaveReqVO reqVO);

    /**
     * 按客户端预期版本修改工序 SOP。
     *
     * @param processId 工序主键
     * @param sopId     SOP 关联主键
     * @param reqVO     修改请求
     */
    void updateProcessSop(Long processId, Long sopId, CraftProcessSopUpdateReqVO reqVO);

    /**
     * 按客户端预期版本逻辑删除工序 SOP。
     *
     * @param processId      工序主键
     * @param sopId          SOP 关联主键
     * @param expectedVersion 客户端读取时的版本号
     */
    void deleteProcessSop(Long processId, Long sopId, Integer expectedVersion);

    /**
     * 查询工序的未删除 SOP。
     *
     * @param processId 工序主键
     * @return SOP 列表
     */
    List<CraftProcessSopRespVO> getProcessSops(Long processId);
}
