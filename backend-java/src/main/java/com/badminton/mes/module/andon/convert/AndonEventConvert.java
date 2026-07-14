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

/** 现场安灯异常显式转换器。 */
public final class AndonEventConvert {

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

    private AndonEventConvert() {
    }
}
