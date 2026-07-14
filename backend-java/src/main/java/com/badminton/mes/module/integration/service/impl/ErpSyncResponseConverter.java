package com.badminton.mes.module.integration.service.impl;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.integration.controller.vo.ErpCraftPendingRespVO;
import com.badminton.mes.module.integration.controller.vo.ErpTaskSyncRespVO;
import com.badminton.mes.module.integration.controller.vo.IntegrationWriteLogRespVO;
import com.badminton.mes.module.integration.dal.entity.ErpCraftPendingEntity;
import com.badminton.mes.module.integration.dal.entity.IntegrationWriteLogEntity;
import com.badminton.mes.module.integration.enums.IntegrationWriteStatusEnum;
import com.badminton.mes.module.integration.service.dto.IntegrationCommandResult;

/**
 * ERP 同步响应转换器，集中处理实体和命令结果到响应对象的无状态转换。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
final class ErpSyncResponseConverter {

    /**
     * 将任务命令结果转换为成功或重复明细。
     *
     * @param erpOrderNo ERP 任务单号
     * @param result     命令结果
     * @return 任务同步明细
     */
    static ErpTaskSyncRespVO.Detail toTaskDetail(
            String erpOrderNo, IntegrationCommandResult result) {
        ErpTaskSyncRespVO.Detail detail = new ErpTaskSyncRespVO.Detail();
        detail.setErpOrderNo(erpOrderNo);
        detail.setStatus(result.duplicate()
                ? IntegrationWriteStatusEnum.DUPLICATE.getCode()
                : IntegrationWriteStatusEnum.SUCCESS.getCode());
        detail.setWorkOrderId(result.businessId());
        detail.setWorkOrderNo(result.businessNo());
        return detail;
    }

    /**
     * 将任务业务异常转换为失败明细。
     *
     * @param erpOrderNo ERP 任务单号
     * @param exception  业务异常
     * @return 任务同步失败明细
     */
    static ErpTaskSyncRespVO.Detail toTaskFailureDetail(
            String erpOrderNo, ServiceException exception) {
        ErpTaskSyncRespVO.Detail detail = new ErpTaskSyncRespVO.Detail();
        detail.setErpOrderNo(erpOrderNo);
        detail.setStatus(IntegrationWriteStatusEnum.FAILED.getCode());
        detail.setErrorCode(exception.getErrorCode().code());
        detail.setErrorMessage(exception.getMessage());
        return detail;
    }

    /**
     * 将同步日志实体转换为响应对象。
     *
     * @param entity 同步日志实体
     * @return 同步日志响应
     */
    static IntegrationWriteLogRespVO toLogResponse(IntegrationWriteLogEntity entity) {
        IntegrationWriteLogRespVO response = new IntegrationWriteLogRespVO();
        response.setId(entity.getId());
        response.setInterfaceType(entity.getInterfaceType());
        response.setSourceSystem(entity.getSourceSystem());
        response.setBusinessKey(entity.getBusinessKey());
        response.setRequestSnapshot(entity.getRequestSnapshot());
        response.setWriteStatus(entity.getWriteStatus());
        response.setResultId(entity.getResultId());
        response.setResultNo(entity.getResultNo());
        response.setErrorCode(entity.getErrorCode());
        response.setErrorMessage(entity.getErrorMessage());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());
        return response;
    }

    /**
     * 将 ERP 工艺暂存实体转换为待确认响应。
     *
     * @param pending ERP 工艺暂存实体
     * @return 待确认响应
     */
    static ErpCraftPendingRespVO toPendingResponse(ErpCraftPendingEntity pending) {
        ErpCraftPendingRespVO response = new ErpCraftPendingRespVO();
        response.setId(pending.getId());
        response.setSourceSystem(pending.getSourceSystem());
        response.setErpRoutingCode(pending.getErpRoutingCode());
        response.setErpRoutingName(pending.getErpRoutingName());
        response.setErpRoutingVersion(pending.getErpRoutingVersion());
        response.setProductCode(pending.getProductCode());
        response.setStatus(pending.getStatus());
        response.setConfirmedRouteId(pending.getConfirmedRouteId());
        response.setErrorCode(pending.getErrorCode());
        response.setErrorMessage(pending.getErrorMessage());
        response.setCreateTime(pending.getCreateTime());
        return response;
    }

    private ErpSyncResponseConverter() {
    }
}
