package com.badminton.mes.module.integration.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.integration.controller.vo.ErpCraftSyncReqVO;
import com.badminton.mes.module.integration.controller.vo.ErpCraftSyncRespVO;
import com.badminton.mes.module.integration.controller.vo.ErpSyncLogPageReqVO;
import com.badminton.mes.module.integration.controller.vo.ErpTaskSyncReqVO;
import com.badminton.mes.module.integration.controller.vo.ErpTaskSyncRespVO;
import com.badminton.mes.module.integration.controller.vo.IntegrationWriteLogRespVO;

/**
 * ERP 同步门面 Service，编排批量同步流程并汇总计数。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
public interface ErpSyncService {

    /**
     * 触发 ERP 生产任务单同步。
     *
     * @param reqVO 同步触发请求
     * @return 同步结果（含逐条明细）
     */
    ErpTaskSyncRespVO syncErpTasks(ErpTaskSyncReqVO reqVO);

    /**
     * 分页查询 ERP 任务同步日志。
     *
     * @param reqVO 分页筛选条件
     * @return 同步日志分页
     */
    PageResult<IntegrationWriteLogRespVO> getErpTaskSyncLogPage(ErpSyncLogPageReqVO reqVO);

    /**
     * 触发 ERP 工艺数据同步。
     *
     * @param reqVO 同步触发请求
     * @return 同步结果（含待确认列表）
     */
    ErpCraftSyncRespVO syncErpCrafts(ErpCraftSyncReqVO reqVO);

    /**
     * 确认待确认工艺数据，生成 MES 工艺路线草稿。
     *
     * @param id 待确认数据主键
     * @return 新生成的工艺路线主键
     */
    Long confirmPendingCraft(Long id);
}
