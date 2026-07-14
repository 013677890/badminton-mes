package com.badminton.mes.module.andon.controller.vo;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 现场安灯异常详情响应。 */
@Data
public class AndonEventRespVO {

    private Long id;
    private String eventNo;
    private Long andonTypeId;
    private String andonTypeCode;
    private String andonTypeName;
    private Long reasonId;
    private Long actualReasonId;
    private String sourceChannel;
    private String severity;
    private Long workshopId;
    private Long productionLineId;
    private Long workOrderId;
    private Long productionTaskId;
    private Long processId;
    private Long equipmentId;
    private Long qualityRecordId;
    private String batchNo;
    private String description;
    private String attachmentUrls;
    private String eventStatus;
    private Long assignedUserId;
    private String assignedRoleCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime responseDeadline;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime escalationDeadline;

    private String timeoutStatus;
    private String lightStatus;
    private String lightMessage;
    private String processingResult;
    private Integer impactMinutes;
    private Integer affectedQuantity;
    private Long initiatedBy;
    private Long confirmedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmedAt;

    private Long completedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    private Long closedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private List<AndonProcessLogRespVO> processLogs;
    private List<AndonNotificationRecordRespVO> notificationRecords;
}
