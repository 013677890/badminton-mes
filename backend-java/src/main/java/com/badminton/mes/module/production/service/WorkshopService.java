package com.badminton.mes.module.production.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.production.controller.vo.ProductionStatusReqVO;
import com.badminton.mes.module.production.controller.vo.WorkshopPageReqVO;
import com.badminton.mes.module.production.controller.vo.WorkshopRespVO;
import com.badminton.mes.module.production.controller.vo.WorkshopSaveReqVO;
import com.badminton.mes.module.production.controller.vo.WorkshopUpdateReqVO;

/**
 * 车间基础资料服务。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
public interface WorkshopService {

    /**
     * 创建车间。
     *
     * @param reqVO 创建请求
     * @return 新车间主键
     */
    Long createWorkshop(WorkshopSaveReqVO reqVO);

    /**
     * 按客户端预期版本修改车间。
     *
     * @param id 车间主键
     * @param reqVO 修改请求
     */
    void updateWorkshop(Long id, WorkshopUpdateReqVO reqVO);

    /**
     * 删除无业务引用的车间。
     *
     * @param id 车间主键
     * @param version 客户端预期版本
     */
    void deleteWorkshop(Long id, Integer version);

    /**
     * 启用或停用车间。
     *
     * @param id 车间主键
     * @param reqVO 状态变更请求
     */
    void updateWorkshopStatus(Long id, ProductionStatusReqVO reqVO);

    /**
     * 查询车间详情。
     *
     * @param id 车间主键
     * @return 车间详情
     */
    WorkshopRespVO getWorkshop(Long id);

    /**
     * 分页查询车间。
     *
     * @param reqVO 分页筛选条件
     * @return 车间分页结果
     */
    PageResult<WorkshopRespVO> getWorkshopPage(WorkshopPageReqVO reqVO);
}
