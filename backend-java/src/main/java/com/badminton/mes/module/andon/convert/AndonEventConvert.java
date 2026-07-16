package com.badminton.mes.module.andon.convert;

import java.util.List;

import com.badminton.mes.module.andon.controller.vo.AndonEventCreateReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonEventRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonNotificationRecordRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonProcessLogRespVO;
import com.badminton.mes.module.andon.dal.entity.AndonEventEntity;
import com.badminton.mes.module.andon.dal.entity.AndonNotificationRecordEntity;
import com.badminton.mes.module.andon.dal.entity.AndonProcessLogEntity;
import com.badminton.mes.module.andon.dal.entity.AndonTypeEntity;

/**
 * 现场安灯事件显式转换器。
 *
 * <p>转换器只负责对象结构映射，不执行状态机、原因归属、责任指派或权限校验。创建转换仅复制调用方
 * 可提交的业务引用和描述字段；事件编号、处理模式、状态、时限、操作者及灯控结果由 Service 在校验
 * 上下文后补齐。响应转换将事件主数据与类型、处理日志、通知记录组装为聚合视图。
 */
public final class AndonEventConvert {

    /**
     * 将创建请求复制为尚未初始化生命周期字段的事件实体。
     *
     * @param request 事件创建请求
     * @return 待 Service 校验引用并补齐状态、责任和审计信息的实体
     */
    public static AndonEventEntity toEntity(AndonEventCreateReqVO request) {
        AndonEventEntity event = new AndonEventEntity();
        event.setAndonTypeId(request.getAndonTypeId());
        event.setReasonId(request.getReasonId());
        event.setSourceChannel(request.getSourceChannel());
        event.setSeverity(request.getSeverity());
        event.setWorkshopId(request.getWorkshopId());
        event.setProductionLineId(request.getProductionLineId());
        event.setWorkOrderId(request.getWorkOrderId());
        event.setProductionTaskId(request.getProductionTaskId());
        event.setProcessId(request.getProcessId());
        event.setEquipmentId(request.getEquipmentId());
        event.setQualityRecordId(request.getQualityRecordId());
        event.setBatchNo(request.getBatchNo());
        event.setDescription(request.getDescription());
        event.setAttachmentUrls(request.getAttachmentUrls());
        return event;
    }

    /**
     * 组装事件聚合响应。
     *
     * <p>类型编码、名称来自已验证的关联类型；日志和通知由调用方决定传入完整集合或空集合，因此
     * 同一转换方法既服务于详情查询，也服务于不加载审计明细的分页摘要查询。
     *
     * @param event 事件主数据
     * @param andonType 事件所属安灯类型
     * @param processLogs 按业务需要加载的处理日志
     * @param notificationRecords 按业务需要加载的通知记录
     * @return 包含主数据及指定审计明细的响应对象
     */
    public static AndonEventRespVO toRespVO(
            AndonEventEntity event,
            AndonTypeEntity andonType,
            List<AndonProcessLogEntity> processLogs,
            List<AndonNotificationRecordEntity> notificationRecords) {
        AndonEventRespVO response = new AndonEventRespVO();
        response.setId(event.getId());
        response.setEventNo(event.getEventNo());
        response.setAndonTypeId(event.getAndonTypeId());
        response.setAndonTypeCode(andonType.getTypeCode());
        response.setAndonTypeName(andonType.getTypeName());
        response.setReasonId(event.getReasonId());
        response.setActualReasonId(event.getActualReasonId());
        response.setSourceChannel(event.getSourceChannel());
        response.setSeverity(event.getSeverity());
        response.setWorkshopId(event.getWorkshopId());
        response.setProductionLineId(event.getProductionLineId());
        response.setWorkOrderId(event.getWorkOrderId());
        response.setProductionTaskId(event.getProductionTaskId());
        response.setProcessId(event.getProcessId());
        response.setEquipmentId(event.getEquipmentId());
        response.setQualityRecordId(event.getQualityRecordId());
        response.setBatchNo(event.getBatchNo());
        response.setDescription(event.getDescription());
        response.setAttachmentUrls(event.getAttachmentUrls());
        response.setEventStatus(event.getEventStatus());
        response.setAssignedUserId(event.getAssignedUserId());
        response.setAssignedRoleCode(event.getAssignedRoleCode());
        response.setResponseDeadline(event.getResponseDeadline());
        response.setEscalationDeadline(event.getEscalationDeadline());
        response.setTimeoutStatus(event.getTimeoutStatus());
        response.setLightStatus(event.getLightStatus());
        response.setLightMessage(event.getLightMessage());
        response.setProcessingResult(event.getProcessingResult());
        response.setImpactMinutes(event.getImpactMinutes());
        response.setAffectedQuantity(event.getAffectedQuantity());
        response.setInitiatedBy(event.getInitiatedBy());
        response.setConfirmedBy(event.getConfirmedBy());
        response.setConfirmedAt(event.getConfirmedAt());
        response.setCompletedBy(event.getCompletedBy());
        response.setCompletedAt(event.getCompletedAt());
        response.setClosedBy(event.getClosedBy());
        response.setClosedAt(event.getClosedAt());
        response.setCreateTime(event.getCreateTime());
        response.setUpdateTime(event.getUpdateTime());
        response.setProcessLogs(processLogs.stream().map(AndonEventConvert::toProcessLogRespVO).toList());
        response.setNotificationRecords(notificationRecords.stream()
                .map(AndonEventConvert::toNotificationRespVO)
                .toList());
        return response;
    }

    /**
     * 转换单条处理日志，保留状态迁移、操作者、目标责任主体和动作内容等审计字段。
     */
    public static AndonProcessLogRespVO toProcessLogRespVO(AndonProcessLogEntity processLog) {
        AndonProcessLogRespVO response = new AndonProcessLogRespVO();
        response.setId(processLog.getId());
        response.setActionType(processLog.getActionType());
        response.setFromStatus(processLog.getFromStatus());
        response.setToStatus(processLog.getToStatus());
        response.setOperatorId(processLog.getOperatorId());
        response.setTargetUserId(processLog.getTargetUserId());
        response.setTargetRoleCode(processLog.getTargetRoleCode());
        response.setActionContent(processLog.getActionContent());
        response.setCreateTime(processLog.getCreateTime());
        return response;
    }

    /**
     * 转换单条模拟通知记录，完整暴露通知类型、渠道、接收主体、发送结果和时间。
     */
    public static AndonNotificationRecordRespVO toNotificationRespVO(
            AndonNotificationRecordEntity notificationRecord) {
        AndonNotificationRecordRespVO response = new AndonNotificationRecordRespVO();
        response.setId(notificationRecord.getId());
        response.setNotificationType(notificationRecord.getNotificationType());
        response.setChannel(notificationRecord.getChannel());
        response.setReceiverUserId(notificationRecord.getReceiverUserId());
        response.setReceiverRoleCode(notificationRecord.getReceiverRoleCode());
        response.setSendStatus(notificationRecord.getSendStatus());
        response.setSendMessage(notificationRecord.getSendMessage());
        response.setSentAt(notificationRecord.getSentAt());
        response.setCreateTime(notificationRecord.getCreateTime());
        return response;
    }

    /** 工具类不允许实例化。 */
    private AndonEventConvert() {
    }
}
