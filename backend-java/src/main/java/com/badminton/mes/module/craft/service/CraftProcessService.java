package com.badminton.mes.module.craft.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.craft.controller.vo.CraftProcessChangeLogPageReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessChangeLogRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessPageReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessSaveReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessStatusReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessUpdateReqVO;

/**
 * 工序主档 Service，承载工序档案、规则、状态和变更追溯业务。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public interface CraftProcessService {

    /**
     * 创建工序。
     *
     * @param reqVO 创建请求
     * @return 新工序主键
     */
    Long createProcess(CraftProcessSaveReqVO reqVO);

    /**
     * 按客户端预期版本修改工序档案与规则。
     *
     * @param id    工序主键
     * @param reqVO 修改请求
     */
    void updateProcess(Long id, CraftProcessUpdateReqVO reqVO);

    /**
     * 按客户端预期版本逻辑删除工序。
     *
     * @param id              工序主键
     * @param expectedVersion 客户端读取时的版本号
     */
    void deleteProcess(Long id, Integer expectedVersion);

    /**
     * 按客户端预期版本启用或停用工序。
     *
     * @param id    工序主键
     * @param reqVO 状态变更请求
     */
    void updateProcessStatus(Long id, CraftProcessStatusReqVO reqVO);

    /**
     * 查询工序详情。
     *
     * @param id 工序主键
     * @return 工序详情
     */
    CraftProcessRespVO getProcess(Long id);

    /**
     * 分页查询工序。
     *
     * @param reqVO 分页查询请求
     * @return 工序分页结果
     */
    PageResult<CraftProcessRespVO> getProcessPage(CraftProcessPageReqVO reqVO);

    /**
     * 分页查询工序变更日志，工序删除后仍可追溯。
     *
     * @param id    工序主键
     * @param reqVO 分页请求
     * @return 变更日志分页结果
     */
    PageResult<CraftProcessChangeLogRespVO> getProcessChangeLogPage(
            Long id, CraftProcessChangeLogPageReqVO reqVO);
}
