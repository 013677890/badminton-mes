package com.badminton.mes.module.andon.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.andon.constants.AndonErrorCodeConstants;
import com.badminton.mes.module.andon.controller.vo.AndonEventActionReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonEventCreateReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonEventPageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonEventRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonProcessLogRespVO;
import com.badminton.mes.module.andon.convert.AndonEventConvert;
import com.badminton.mes.module.andon.dal.entity.AndonConfigurationEntity;
import com.badminton.mes.module.andon.dal.entity.AndonEventEntity;
import com.badminton.mes.module.andon.dal.entity.AndonNotificationRecordEntity;
import com.badminton.mes.module.andon.dal.entity.AndonProcessLogEntity;
import com.badminton.mes.module.andon.dal.entity.AndonReasonEntity;
import com.badminton.mes.module.andon.dal.entity.AndonTypeEntity;
import com.badminton.mes.module.andon.dal.redis.AndonCache;
import com.badminton.mes.module.andon.dal.redis.AndonRedisKeyConstants;
import com.badminton.mes.module.andon.dal.repository.AndonConfigurationRepository;
import com.badminton.mes.module.andon.dal.repository.AndonEventRepository;
import com.badminton.mes.module.andon.dal.repository.AndonEventSpecifications;
import com.badminton.mes.module.andon.dal.repository.AndonNotificationRecordRepository;
import com.badminton.mes.module.andon.dal.repository.AndonProcessLogRepository;
import com.badminton.mes.module.andon.dal.repository.AndonReasonRepository;
import com.badminton.mes.module.andon.dal.repository.AndonTypeRepository;
import com.badminton.mes.module.andon.service.AndonEventService;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerRespVO;
import com.badminton.mes.module.equipment.service.EquipmentLedgerService;
import com.badminton.mes.module.production.controller.vo.WorkOrderRespVO;
import com.badminton.mes.module.production.service.WorkshopService;
import com.badminton.mes.module.production.service.WorkOrderService;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordRespVO;
import com.badminton.mes.module.quality.service.QualityInspectionRecordService;
import com.badminton.mes.module.system.controller.vo.RoleRespVO;
import com.badminton.mes.module.system.controller.vo.UserRespVO;
import com.badminton.mes.module.system.service.RoleService;
import com.badminton.mes.module.system.service.UserService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/** 现场安灯异常 Service 实现。 */
@Service
public class AndonEventServiceImpl implements AndonEventService {

    private static final int ENABLED = 1;
    private static final long GLOBAL_SCOPE_LINE_ID = 0L;
    private static final Long DEFAULT_OPERATOR_ID = 1L;
    private static final String HANDLING_MODE_NO_ACTION = "NO_ACTION";
    private static final String HANDLING_MODE_SELF_HANDLE = "SELF_HANDLE";
    private static final String HANDLING_MODE_ASSISTANCE = "ASSISTANCE";
    private static final String STATUS_PENDING_CONFIRMATION = "PENDING_CONFIRMATION";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_WAITING_CLOSE = "WAITING_CLOSE";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String TIMEOUT_NORMAL = "NORMAL";
    private static final String TIMEOUT_RESPONSE_OVERDUE = "RESPONSE_OVERDUE";
    private static final String TIMEOUT_ESCALATED = "ESCALATED";
    private static final String LIGHT_NOT_REQUIRED = "NOT_REQUIRED";
    private static final String LIGHT_ON = "ON";
    private static final String LIGHT_OFF = "OFF";
    private static final String LIGHT_FAILED = "FAILED";
    private static final String NOTIFICATION_SIMULATED = "SIMULATED";
    private static final Set<String> MANAGEMENT_ROLE_CODES = Set.of(
            "ADMIN", "WORKSHOP_MANAGER", "TEAM_LEADER");
    private static final Set<Integer> ACTIVE_WORK_ORDER_STATUSES = Set.of(1, 2, 3);
    private static final DateTimeFormatter EVENT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final AndonEventRepository eventRepository;
    private final AndonProcessLogRepository processLogRepository;
    private final AndonNotificationRecordRepository notificationRepository;
    private final AndonTypeRepository typeRepository;
    private final AndonReasonRepository reasonRepository;
    private final AndonConfigurationRepository configurationRepository;
    private final WorkOrderService workOrderService;
    private final WorkshopService workshopService;
    private final EquipmentLedgerService equipmentLedgerService;
    private final QualityInspectionRecordService qualityRecordService;
    private final UserService userService;
    private final RoleService roleService;
    private final AndonCache andonCache;
    private final TransactionTemplate timeoutTransactionTemplate;

    public AndonEventServiceImpl(
            AndonEventRepository eventRepository,
            AndonProcessLogRepository processLogRepository,
            AndonNotificationRecordRepository notificationRepository,
            AndonTypeRepository typeRepository,
            AndonReasonRepository reasonRepository,
            AndonConfigurationRepository configurationRepository,
            WorkOrderService workOrderService,
            WorkshopService workshopService,
            EquipmentLedgerService equipmentLedgerService,
            QualityInspectionRecordService qualityRecordService,
            UserService userService,
            RoleService roleService,
            AndonCache andonCache,
            PlatformTransactionManager transactionManager) {
        this.eventRepository = eventRepository;
        this.processLogRepository = processLogRepository;
        this.notificationRepository = notificationRepository;
        this.typeRepository = typeRepository;
        this.reasonRepository = reasonRepository;
        this.configurationRepository = configurationRepository;
        this.workOrderService = workOrderService;
        this.workshopService = workshopService;
        this.equipmentLedgerService = equipmentLedgerService;
        this.qualityRecordService = qualityRecordService;
        this.userService = userService;
        this.roleService = roleService;
        this.andonCache = andonCache;
        this.timeoutTransactionTemplate = new TransactionTemplate(transactionManager);
        this.timeoutTransactionTemplate.setPropagationBehaviorName("PROPAGATION_REQUIRES_NEW");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEvent(AndonEventCreateReqVO request) {
        AndonTypeEntity andonType = getEnabledTypeForUpdate(request.getAndonTypeId());
        validateReasonForUpdate(request.getReasonId(), request.getAndonTypeId());
        AndonEventEntity event = AndonEventConvert.toEntity(request);
        validateAndEnrichReferences(event);
        LocalDateTime now = LocalDateTime.now();
        Long initiatorId = getCurrentOperatorId();
        event.setEventNo(generateEventNo(now));
        event.setSeverity(StringUtils.hasText(request.getSeverity()) ? request.getSeverity() : "NORMAL");
        event.setTimeoutStatus(TIMEOUT_NORMAL);
        applyInitialLightStatus(event, andonType);
        event.setInitiatedBy(initiatorId);
        event.setDeleted(false);

        AssistanceRule assistanceRule = null;
        if (HANDLING_MODE_ASSISTANCE.equals(andonType.getHandlingMode())) {
            assistanceRule = prepareAssistanceEvent(event, andonType, now);
        } else if (HANDLING_MODE_SELF_HANDLE.equals(andonType.getHandlingMode())) {
            event.setEventStatus(STATUS_PENDING_CONFIRMATION);
            event.setAssignedUserId(initiatorId);
        } else if (HANDLING_MODE_NO_ACTION.equals(andonType.getHandlingMode())) {
            prepareNoActionEvent(event, now);
        } else {
            throw new ServiceException(AndonErrorCodeConstants.TYPE_RULE_INVALID);
        }

        saveEvent(event);
        saveProcessLog(event, "INITIATE", null, event.getEventStatus(),
                event.getInitiatedBy(), event.getAssignedUserId(), event.getAssignedRoleCode(), request.getDescription());
        if (assistanceRule != null) {
            saveNotifications(event, assistanceRule.notificationChannels(), "INITIAL",
                    event.getAssignedUserId(), event.getAssignedRoleCode(), "安灯异常已发起：" + event.getEventNo());
        }
        return event.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmEvent(Long id, AndonEventActionReqVO request) {
        AndonEventEntity event = getEventForUpdate(id);
        requireStatus(event, STATUS_PENDING_CONFIRMATION);
        requireCanHandle(event);
        validateReason(request.getActualReasonId(), event.getAndonTypeId());
        String previousStatus = event.getEventStatus();
        event.setActualReasonId(request.getActualReasonId());
        event.setEventStatus(STATUS_CONFIRMED);
        event.setConfirmedBy(getCurrentOperatorId());
        event.setConfirmedAt(LocalDateTime.now());
        event.setResponseDeadline(null);
        saveEvent(event);
        saveProcessLog(event, "CONFIRM", previousStatus, STATUS_CONFIRMED,
                event.getConfirmedBy(), event.getAssignedUserId(), event.getAssignedRoleCode(), request.getActionContent());
        saveStatusNotifications(event, "异常已确认");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startProcessing(Long id, AndonEventActionReqVO request) {
        AndonEventEntity event = getEventForUpdate(id);
        requireStatus(event, STATUS_CONFIRMED);
        requireCanHandle(event);
        transitionSimple(event, request, STATUS_PROCESSING, "START_PROCESS", "异常开始处理");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferEvent(Long id, AndonEventActionReqVO request) {
        AndonEventEntity event = getEventForUpdate(id);
        requireAnyStatus(event, STATUS_CONFIRMED, STATUS_PROCESSING);
        requireCanHandle(event);
        if (!StringUtils.hasText(request.getActionContent())) {
            throw new ServiceException(AndonErrorCodeConstants.EVENT_RESULT_INCOMPLETE);
        }
        validateAssignment(request.getTargetUserId(), request.getTargetRoleCode());
        event.setAssignedUserId(request.getTargetUserId());
        event.setAssignedRoleCode(request.getTargetRoleCode());
        saveEvent(event);
        saveProcessLog(event, "TRANSFER", event.getEventStatus(), event.getEventStatus(),
                getCurrentOperatorId(), request.getTargetUserId(), request.getTargetRoleCode(), request.getActionContent());
        saveStatusNotifications(event, "异常已转派");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeEvent(Long id, AndonEventActionReqVO request) {
        AndonEventEntity event = getEventForUpdate(id);
        requireStatus(event, STATUS_PROCESSING);
        requireCanHandle(event);
        validateCompletionRequest(event, request);
        String previousStatus = event.getEventStatus();
        event.setActualReasonId(request.getActualReasonId() == null
                ? event.getActualReasonId() : request.getActualReasonId());
        event.setProcessingResult(request.getProcessingResult());
        event.setImpactMinutes(request.getImpactMinutes());
        event.setAffectedQuantity(request.getAffectedQuantity());
        event.setEventStatus(STATUS_WAITING_CLOSE);
        event.setCompletedBy(getCurrentOperatorId());
        event.setCompletedAt(LocalDateTime.now());
        event.setResponseDeadline(null);
        event.setEscalationDeadline(null);
        saveEvent(event);
        saveProcessLog(event, "COMPLETE", previousStatus, STATUS_WAITING_CLOSE,
                event.getCompletedBy(), event.getAssignedUserId(), event.getAssignedRoleCode(), request.getActionContent());
        saveStatusNotifications(event, "异常处理已完成，等待关闭");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeEvent(Long id, AndonEventActionReqVO request) {
        AndonEventEntity event = getEventForUpdate(id);
        requireStatus(event, STATUS_WAITING_CLOSE);
        requireManagementOperator();
        String previousStatus = event.getEventStatus();
        event.setEventStatus(STATUS_CLOSED);
        event.setClosedBy(getCurrentOperatorId());
        event.setClosedAt(LocalDateTime.now());
        event.setResponseDeadline(null);
        event.setEscalationDeadline(null);
        if (LIGHT_ON.equals(event.getLightStatus())) {
            event.setLightStatus(LIGHT_OFF);
            event.setLightMessage("模拟关闭设备安灯成功");
        }
        saveEvent(event);
        saveProcessLog(event, "CLOSE", previousStatus, STATUS_CLOSED,
                event.getClosedBy(), event.getAssignedUserId(), event.getAssignedRoleCode(), request.getActionContent());
        saveStatusNotifications(event, "异常已关闭");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void escalateEvent(Long id, AndonEventActionReqVO request) {
        AndonEventEntity event = getEventForUpdate(id);
        requireAnyStatus(event, STATUS_CONFIRMED, STATUS_PROCESSING);
        if (TIMEOUT_ESCALATED.equals(event.getTimeoutStatus())) {
            throw new ServiceException(AndonErrorCodeConstants.EVENT_STATUS_INVALID);
        }
        requireCanHandle(event);
        Optional<AssistanceRule> assistanceRule = findBestEffortAssistanceRule(
                event.getAndonTypeId(), event.getProductionLineId());
        Long targetUserId = request.getTargetUserId() == null
                ? assistanceRule.map(AssistanceRule::escalationUserId).orElse(null)
                : request.getTargetUserId();
        String targetRoleCode = StringUtils.hasText(request.getTargetRoleCode())
                ? request.getTargetRoleCode()
                : assistanceRule.map(AssistanceRule::escalationRoleCode).orElse(null);
        validateAssignment(targetUserId, targetRoleCode);
        event.setAssignedUserId(targetUserId);
        event.setAssignedRoleCode(targetRoleCode);
        event.setTimeoutStatus(TIMEOUT_ESCALATED);
        event.setResponseDeadline(null);
        event.setEscalationDeadline(null);
        saveEvent(event);
        saveProcessLog(event, "ESCALATE", event.getEventStatus(), event.getEventStatus(),
                getCurrentOperatorId(), targetUserId, targetRoleCode, request.getActionContent());
        assistanceRule.ifPresent(rule -> saveNotifications(
                event,
                rule.notificationChannels(),
                "ESCALATION",
                targetUserId,
                targetRoleCode,
                "安灯异常已升级：" + event.getEventNo()));
    }

    @Override
    @Transactional(readOnly = true)
    public int processTimeoutEvents() {
        LocalDateTime now = LocalDateTime.now();
        int processedEventCount = 0;
        for (AndonEventEntity timeoutCandidate : eventRepository.findTimeoutCandidates(now)) {
            try {
                Boolean processed = timeoutTransactionTemplate.execute(status ->
                        processSingleTimeoutEvent(timeoutCandidate.getId(), now));
                if (Boolean.TRUE.equals(processed)) {
                    processedEventCount++;
                }
            } catch (RuntimeException exception) {
                recordTimeoutProcessingFailure(timeoutCandidate.getId(), exception);
            }
        }
        return processedEventCount;
    }

    private void recordTimeoutProcessingFailure(Long eventId, RuntimeException exception) {
        try {
            timeoutTransactionTemplate.executeWithoutResult(status -> {
                AndonEventEntity event = getEventForUpdate(eventId);
                if (STATUS_CLOSED.equals(event.getEventStatus())) {
                    return;
                }
                event.setResponseDeadline(null);
                event.setEscalationDeadline(null);
                saveEvent(event);
                saveProcessLog(
                        event,
                        "ESCALATE",
                        event.getEventStatus(),
                        event.getEventStatus(),
                        DEFAULT_OPERATOR_ID,
                        event.getAssignedUserId(),
                        event.getAssignedRoleCode(),
                        "系统超时处理失败：" + getFailureMessage(exception));
            });
        } catch (RuntimeException ignoredException) {
            // 该事件会保留原状态，下一轮扫描仍可重试；其他事件不受影响。
        }
    }

    private String getFailureMessage(RuntimeException exception) {
        String failureMessage = exception.getMessage();
        return StringUtils.hasText(failureMessage) ? failureMessage : exception.getClass().getSimpleName();
    }

    private boolean processSingleTimeoutEvent(Long eventId, LocalDateTime now) {
        AndonEventEntity event = getEventForUpdate(eventId);
        if (STATUS_CLOSED.equals(event.getEventStatus())) {
            return false;
        }
        if (shouldEscalate(event, now)) {
            return escalateTimeoutEvent(event);
        }
        if (!shouldMarkResponseOverdue(event, now)) {
            return false;
        }
        event.setTimeoutStatus(TIMEOUT_RESPONSE_OVERDUE);
        event.setResponseDeadline(null);
        saveEvent(event);
        saveProcessLog(
                event,
                "ESCALATE",
                event.getEventStatus(),
                event.getEventStatus(),
                DEFAULT_OPERATOR_ID,
                event.getAssignedUserId(),
                event.getAssignedRoleCode(),
                "系统标记响应超时");
        saveStatusNotifications(event, "异常响应已超时");
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public AndonEventRespVO getEvent(Long id) {
        return andonCache.getOrLoadDetail(AndonRedisKeyConstants.EVENT_RESOURCE,
                id, AndonEventRespVO.class, () -> {
            AndonEventEntity event = getEventEntity(id);
            AndonEventRespVO response = AndonEventConvert.toRespVO(
                    event,
                    getType(event.getAndonTypeId()),
                    processLogRepository.findByEventIdOrderByIdAsc(id),
                    notificationRepository.findByEventIdOrderByIdAsc(id));
            return response;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AndonEventRespVO> getEventPage(AndonEventPageReqVO request) {
        var specification = AndonEventSpecifications.page(request);
        long total = eventRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(request.getPageNo(), request.getPageSize());
        }
        int pageSize = request.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(request.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<AndonEventEntity> page = eventRepository.findAll(specification, pageRequest);
        Map<Long, AndonTypeEntity> typesById = typeRepository.findAllById(
                        page.getContent().stream().map(AndonEventEntity::getAndonTypeId).distinct().toList())
                .stream().collect(Collectors.toMap(AndonTypeEntity::getId, Function.identity()));
        List<AndonEventRespVO> list = page.getContent().stream()
                .map(event -> AndonEventConvert.toRespVO(
                        event, requireType(typesById, event.getAndonTypeId()), List.of(), List.of()))
                .toList();
        return PageResult.of(list, total, pageNo, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AndonProcessLogRespVO> getProcessLogs(Long id) {
        getEventEntity(id);
        return processLogRepository.findByEventIdOrderByIdAsc(id).stream()
                .map(AndonEventConvert::toProcessLogRespVO)
                .toList();
    }

    private void transitionSimple(
            AndonEventEntity event,
            AndonEventActionReqVO request,
            String targetStatus,
            String actionType,
            String notificationMessage) {
        String previousStatus = event.getEventStatus();
        event.setEventStatus(targetStatus);
        saveEvent(event);
        saveProcessLog(event, actionType, previousStatus, targetStatus,
                getCurrentOperatorId(), event.getAssignedUserId(), event.getAssignedRoleCode(), request.getActionContent());
        saveStatusNotifications(event, notificationMessage);
    }

    private AssistanceRule prepareAssistanceEvent(
            AndonEventEntity event,
            AndonTypeEntity andonType,
            LocalDateTime initiatedAt) {
        AssistanceRule assistanceRule = resolveAssistanceRuleForCreate(
                andonType, event.getProductionLineId());
        validateAssignment(assistanceRule.handlerUserId(), assistanceRule.handlerRoleCode());
        event.setEventStatus(STATUS_PENDING_CONFIRMATION);
        event.setAssignedUserId(assistanceRule.handlerUserId());
        event.setAssignedRoleCode(assistanceRule.handlerRoleCode());
        event.setResponseDeadline(initiatedAt.plusMinutes(assistanceRule.responseMinutes()));
        if (assistanceRule.escalationMinutes() != null) {
            event.setEscalationDeadline(initiatedAt.plusMinutes(assistanceRule.escalationMinutes()));
        }
        return assistanceRule;
    }

    private void prepareNoActionEvent(AndonEventEntity event, LocalDateTime completedAt) {
        event.setActualReasonId(event.getReasonId());
        event.setEventStatus(STATUS_CLOSED);
        event.setProcessingResult("该安灯类型无需处理，系统已自动闭环");
        event.setConfirmedBy(event.getInitiatedBy());
        event.setConfirmedAt(completedAt);
        event.setCompletedBy(event.getInitiatedBy());
        event.setCompletedAt(completedAt);
        event.setClosedBy(event.getInitiatedBy());
        event.setClosedAt(completedAt);
        if (LIGHT_ON.equals(event.getLightStatus())) {
            event.setLightStatus(LIGHT_OFF);
            event.setLightMessage("无需处理，模拟开启后立即关闭设备安灯");
        }
    }

    private void validateAndEnrichReferences(AndonEventEntity event) {
        if (event.getProductionTaskId() != null || event.getProcessId() != null) {
            throw new ServiceException(AndonErrorCodeConstants.EVENT_REFERENCE_UNSUPPORTED);
        }

        boolean productionLineValidatedBySource = false;
        if (event.getQualityRecordId() != null) {
            QualityInspectionRecordRespVO qualityRecord = qualityRecordService.getRecord(event.getQualityRecordId());
            if (!"SUBMITTED".equals(qualityRecord.getRecordStatus())) {
                throw new ServiceException(AndonErrorCodeConstants.EVENT_REFERENCE_INVALID);
            }
            event.setWorkOrderId(reconcileId(event.getWorkOrderId(), qualityRecord.getWorkOrderId()));
            event.setProductionLineId(reconcileId(event.getProductionLineId(), qualityRecord.getProductionLineId()));
            event.setBatchNo(reconcileText(event.getBatchNo(), qualityRecord.getBatchNo()));
            productionLineValidatedBySource = qualityRecord.getProductionLineId() != null;
        }
        if (event.getWorkOrderId() != null) {
            WorkOrderRespVO workOrder = workOrderService.getWorkOrder(event.getWorkOrderId());
            if (!ACTIVE_WORK_ORDER_STATUSES.contains(workOrder.getOrderStatus())) {
                throw new ServiceException(AndonErrorCodeConstants.EVENT_REFERENCE_INVALID);
            }
            event.setWorkshopId(reconcileId(event.getWorkshopId(), workOrder.getWorkshopId()));
            event.setBatchNo(reconcileText(event.getBatchNo(), workOrder.getBatchNo()));
        }
        if (event.getEquipmentId() != null) {
            EquipmentLedgerRespVO equipment = equipmentLedgerService.getEquipmentLedger(event.getEquipmentId());
            if (!Integer.valueOf(ENABLED).equals(equipment.getStatus())
                    || "SCRAPPED".equals(equipment.getEquipmentStatus())) {
                throw new ServiceException(AndonErrorCodeConstants.EVENT_REFERENCE_INVALID);
            }
            event.setWorkshopId(reconcileId(event.getWorkshopId(), equipment.getWorkshopId()));
            event.setProductionLineId(reconcileId(event.getProductionLineId(), equipment.getProductionLineId()));
            productionLineValidatedBySource = productionLineValidatedBySource
                    || equipment.getProductionLineId() != null;
        }
        if (event.getProductionLineId() != null && !productionLineValidatedBySource) {
            throw new ServiceException(AndonErrorCodeConstants.EVENT_REFERENCE_UNSUPPORTED);
        }
        if (event.getWorkshopId() != null) {
            workshopService.getEnabledWorkshop(event.getWorkshopId());
        }
    }

    private Long reconcileId(Long requestedId, Long trustedId) {
        if (requestedId != null && trustedId != null && !requestedId.equals(trustedId)) {
            throw new ServiceException(AndonErrorCodeConstants.EVENT_REFERENCE_INVALID);
        }
        return trustedId == null ? requestedId : trustedId;
    }

    private String reconcileText(String requestedText, String trustedText) {
        if (StringUtils.hasText(requestedText) && StringUtils.hasText(trustedText)
                && !requestedText.equals(trustedText)) {
            throw new ServiceException(AndonErrorCodeConstants.EVENT_REFERENCE_INVALID);
        }
        return StringUtils.hasText(trustedText) ? trustedText : requestedText;
    }

    private AssistanceRule resolveAssistanceRuleForCreate(
            AndonTypeEntity andonType,
            Long productionLineId) {
        if (productionLineId != null) {
            List<AndonConfigurationEntity> lineConfigurations = configurationRepository
                    .findActiveLineConfigurationsForUpdate(
                            andonType.getId(), productionLineId, ENABLED);
            if (!lineConfigurations.isEmpty()) {
                return toAssistanceRule(lineConfigurations.getFirst());
            }
        }
        List<AndonConfigurationEntity> globalConfigurations = configurationRepository
                .findActiveScopeConfigurationsForUpdate(
                        andonType.getId(), GLOBAL_SCOPE_LINE_ID, ENABLED);
        if (!globalConfigurations.isEmpty()) {
            return toAssistanceRule(globalConfigurations.getFirst());
        }
        return toTypeDefaultAssistanceRule(andonType);
    }

    private Optional<AssistanceRule> findBestEffortAssistanceRule(
            Long typeId,
            Long productionLineId) {
        if (productionLineId != null) {
            Optional<AndonConfigurationEntity> activeLineConfiguration = configurationRepository
                    .findFirstByAndonTypeIdAndProductionLineIdAndEnabledStatusAndDeletedFalseOrderByIdDesc(
                            typeId, productionLineId, ENABLED);
            if (activeLineConfiguration.isPresent()) {
                return activeLineConfiguration.map(this::toAssistanceRule);
            }
        }
        Optional<AndonConfigurationEntity> activeGlobalConfiguration = configurationRepository
                .findFirstByAndonTypeIdAndScopeLineIdAndEnabledStatusAndDeletedFalseOrderByIdDesc(
                        typeId, GLOBAL_SCOPE_LINE_ID, ENABLED);
        if (activeGlobalConfiguration.isPresent()) {
            return activeGlobalConfiguration.map(this::toAssistanceRule);
        }
        Optional<AssistanceRule> historicalConfigurationRule = configurationRepository
                .findByAndonTypeIdAndDeletedFalse(typeId).stream()
                .filter(configuration -> configuration.getScopeLineId() == GLOBAL_SCOPE_LINE_ID
                        || productionLineId != null
                        && productionLineId.equals(configuration.getProductionLineId()))
                .max((firstConfiguration, secondConfiguration) ->
                        Long.compare(firstConfiguration.getId(), secondConfiguration.getId()))
                .map(this::toAssistanceRule);
        if (historicalConfigurationRule.isPresent()) {
            return historicalConfigurationRule;
        }
        return typeRepository.findByIdAndDeletedFalse(typeId)
                .filter(type -> HANDLING_MODE_ASSISTANCE.equals(type.getHandlingMode()))
                .map(this::toTypeDefaultAssistanceRule);
    }

    private AssistanceRule toAssistanceRule(AndonConfigurationEntity configuration) {
        return new AssistanceRule(
                configuration.getHandlerUserId(),
                configuration.getHandlerRoleCode(),
                configuration.getEscalationUserId(),
                configuration.getEscalationRoleCode(),
                configuration.getResponseMinutes(),
                configuration.getEscalationMinutes(),
                configuration.getNotificationChannels());
    }

    private AssistanceRule toTypeDefaultAssistanceRule(AndonTypeEntity andonType) {
        if (andonType.getResponseMinutes() == null
                || !StringUtils.hasText(andonType.getResponsibleRoleCode())
                || !StringUtils.hasText(andonType.getNotificationChannels())) {
            throw new ServiceException(AndonErrorCodeConstants.EVENT_CONFIGURATION_NOT_MATCHED);
        }
        return new AssistanceRule(
                null,
                andonType.getResponsibleRoleCode(),
                null,
                null,
                andonType.getResponseMinutes(),
                null,
                andonType.getNotificationChannels());
    }

    private boolean shouldEscalate(AndonEventEntity event, LocalDateTime now) {
        return !TIMEOUT_ESCALATED.equals(event.getTimeoutStatus())
                && event.getEscalationDeadline() != null
                && !event.getEscalationDeadline().isAfter(now);
    }

    private boolean shouldMarkResponseOverdue(AndonEventEntity event, LocalDateTime now) {
        return STATUS_PENDING_CONFIRMATION.equals(event.getEventStatus())
                && TIMEOUT_NORMAL.equals(event.getTimeoutStatus())
                && event.getResponseDeadline() != null
                && !event.getResponseDeadline().isAfter(now);
    }

    private boolean escalateTimeoutEvent(AndonEventEntity event) {
        Optional<AssistanceRule> assistanceRule = findBestEffortAssistanceRule(
                event.getAndonTypeId(), event.getProductionLineId());
        Long escalationUserId = assistanceRule
                .map(AssistanceRule::escalationUserId)
                .orElse(null);
        String escalationRoleCode = assistanceRule
                .map(AssistanceRule::escalationRoleCode)
                .orElse(null);
        if (escalationUserId == null && !StringUtils.hasText(escalationRoleCode)) {
            event.setEscalationDeadline(null);
            saveEvent(event);
            return false;
        }
        validateAssignment(escalationUserId, escalationRoleCode);
        event.setAssignedUserId(escalationUserId);
        event.setAssignedRoleCode(escalationRoleCode);
        event.setTimeoutStatus(TIMEOUT_ESCALATED);
        event.setResponseDeadline(null);
        event.setEscalationDeadline(null);
        saveEvent(event);
        saveProcessLog(
                event,
                "ESCALATE",
                event.getEventStatus(),
                event.getEventStatus(),
                DEFAULT_OPERATOR_ID,
                escalationUserId,
                escalationRoleCode,
                "系统执行超时升级");
        assistanceRule.ifPresent(rule -> saveNotifications(
                event,
                rule.notificationChannels(),
                "ESCALATION",
                escalationUserId,
                escalationRoleCode,
                "安灯异常已自动升级：" + event.getEventNo()));
        return true;
    }

    private void validateReason(Long reasonId, Long typeId) {
        validateReasonForUpdate(reasonId, typeId);
    }

    private void validateReasonForUpdate(Long reasonId, Long typeId) {
        if (reasonId == null) {
            return;
        }
        AndonReasonEntity reason = reasonRepository.findByIdAndDeletedFalseForUpdate(reasonId)
                .orElseThrow(() -> new ServiceException(AndonErrorCodeConstants.REASON_NOT_EXISTS));
        if (!typeId.equals(reason.getAndonTypeId()) || !Integer.valueOf(ENABLED).equals(reason.getEnabledStatus())) {
            throw new ServiceException(AndonErrorCodeConstants.EVENT_REFERENCE_INVALID);
        }
    }

    private void validateAssignment(Long userId, String roleCode) {
        if (userId == null && !StringUtils.hasText(roleCode)) {
            throw new ServiceException(AndonErrorCodeConstants.EVENT_ASSIGNEE_INVALID);
        }
        if (userId != null) {
            UserRespVO user = userService.getUser(userId);
            if (!Integer.valueOf(ENABLED).equals(user.getStatus())) {
                throw new ServiceException(AndonErrorCodeConstants.EVENT_ASSIGNEE_INVALID);
            }
        }
        if (StringUtils.hasText(roleCode)) {
            boolean roleExists = roleService.getEnabledRoles().stream()
                    .map(RoleRespVO::getRoleCode)
                    .anyMatch(roleCode::equals);
            if (!roleExists) {
                throw new ServiceException(AndonErrorCodeConstants.EVENT_ASSIGNEE_INVALID);
            }
        }
    }

    private void validateCompletionRequest(AndonEventEntity event, AndonEventActionReqVO request) {
        Long actualReasonId = request.getActualReasonId() == null
                ? event.getActualReasonId() : request.getActualReasonId();
        if (actualReasonId == null || !StringUtils.hasText(request.getProcessingResult())) {
            throw new ServiceException(AndonErrorCodeConstants.EVENT_RESULT_INCOMPLETE);
        }
        validateReason(actualReasonId, event.getAndonTypeId());
    }

    private void applyInitialLightStatus(AndonEventEntity event, AndonTypeEntity andonType) {
        if (!Boolean.TRUE.equals(andonType.getLightControlEnabled())) {
            event.setLightStatus(LIGHT_NOT_REQUIRED);
            event.setLightMessage("该安灯类型未启用设备灯控");
        } else if (event.getEquipmentId() == null) {
            event.setLightStatus(LIGHT_FAILED);
            event.setLightMessage("未关联设备，已跳过模拟灯控");
        } else {
            event.setLightStatus(LIGHT_ON);
            event.setLightMessage("模拟开启设备安灯成功");
        }
    }

    private void saveStatusNotifications(AndonEventEntity event, String message) {
        findBestEffortAssistanceRule(event.getAndonTypeId(), event.getProductionLineId())
                .ifPresent(rule -> saveNotifications(
                        event,
                        rule.notificationChannels(),
                        "STATUS",
                        event.getAssignedUserId(),
                        event.getAssignedRoleCode(),
                        message + "：" + event.getEventNo()));
    }

    private record AssistanceRule(
            Long handlerUserId,
            String handlerRoleCode,
            Long escalationUserId,
            String escalationRoleCode,
            Integer responseMinutes,
            Integer escalationMinutes,
            String notificationChannels) {
    }

    private void saveNotifications(AndonEventEntity event, String channels, String notificationType,
                                   Long receiverUserId, String receiverRoleCode, String message) {
        if (!StringUtils.hasText(channels)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<AndonNotificationRecordEntity> notifications = List.of(channels.split(",")).stream()
                .map(channel -> createNotification(event.getId(), notificationType, channel,
                        receiverUserId, receiverRoleCode, message, now))
                .toList();
        notificationRepository.saveAll(notifications);
    }

    private AndonNotificationRecordEntity createNotification(
            Long eventId, String notificationType, String channel, Long receiverUserId,
            String receiverRoleCode, String message, LocalDateTime sentAt) {
        AndonNotificationRecordEntity notification = new AndonNotificationRecordEntity();
        notification.setEventId(eventId);
        notification.setNotificationType(notificationType);
        notification.setChannel(channel);
        notification.setReceiverUserId(receiverUserId);
        notification.setReceiverRoleCode(receiverRoleCode);
        notification.setSendStatus(NOTIFICATION_SIMULATED);
        notification.setSendMessage(message);
        notification.setSentAt(sentAt);
        return notification;
    }

    private void saveProcessLog(AndonEventEntity event, String actionType, String fromStatus,
                                String toStatus, Long operatorId, Long targetUserId,
                                String targetRoleCode, String actionContent) {
        AndonProcessLogEntity processLog = new AndonProcessLogEntity();
        processLog.setEventId(event.getId());
        processLog.setActionType(actionType);
        processLog.setFromStatus(fromStatus);
        processLog.setToStatus(toStatus);
        processLog.setOperatorId(operatorId);
        processLog.setTargetUserId(targetUserId);
        processLog.setTargetRoleCode(targetRoleCode);
        processLog.setActionContent(actionContent);
        processLogRepository.save(processLog);
    }

    private String generateEventNo(LocalDateTime now) {
        return "AND" + EVENT_TIME_FORMATTER.format(now)
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private void requireStatus(AndonEventEntity event, String requiredStatus) {
        if (!requiredStatus.equals(event.getEventStatus())) {
            throw new ServiceException(AndonErrorCodeConstants.EVENT_STATUS_INVALID);
        }
    }

    private void requireAnyStatus(AndonEventEntity event, String... allowedStatuses) {
        if (!Set.of(allowedStatuses).contains(event.getEventStatus())) {
            throw new ServiceException(AndonErrorCodeConstants.EVENT_STATUS_INVALID);
        }
    }

    private void requireCanHandle(AndonEventEntity event) {
        LoginUser loginUser = SecurityContextHolder.getLoginUser();
        if (loginUser == null || hasManagementRole(loginUser)) {
            return;
        }
        boolean isAssignedUser = event.getAssignedUserId() != null
                && event.getAssignedUserId().equals(loginUser.getUserId());
        boolean hasAssignedRole = StringUtils.hasText(event.getAssignedRoleCode())
                && loginUser.getRoleCodes() != null
                && loginUser.getRoleCodes().contains(event.getAssignedRoleCode());
        if (!isAssignedUser && !hasAssignedRole) {
            throw new ServiceException(AndonErrorCodeConstants.EVENT_OPERATION_FORBIDDEN);
        }
    }

    private void requireManagementOperator() {
        LoginUser loginUser = SecurityContextHolder.getLoginUser();
        if (loginUser == null || hasManagementRole(loginUser)) {
            return;
        }
        throw new ServiceException(AndonErrorCodeConstants.EVENT_OPERATION_FORBIDDEN);
    }

    private boolean hasManagementRole(LoginUser loginUser) {
        return loginUser.getRoleCodes() != null
                && loginUser.getRoleCodes().stream().anyMatch(MANAGEMENT_ROLE_CODES::contains);
    }

    private AndonTypeEntity getEnabledType(Long typeId) {
        AndonTypeEntity andonType = getType(typeId);
        if (!Integer.valueOf(ENABLED).equals(andonType.getEnabledStatus())) {
            throw new ServiceException(AndonErrorCodeConstants.TYPE_NOT_EXISTS);
        }
        return andonType;
    }

    private AndonTypeEntity getEnabledTypeForUpdate(Long typeId) {
        AndonTypeEntity andonType = typeRepository.findByIdAndDeletedFalseForUpdate(typeId)
                .orElseThrow(() -> new ServiceException(AndonErrorCodeConstants.TYPE_NOT_EXISTS));
        if (!Integer.valueOf(ENABLED).equals(andonType.getEnabledStatus())) {
            throw new ServiceException(AndonErrorCodeConstants.TYPE_NOT_EXISTS);
        }
        return andonType;
    }

    private AndonTypeEntity getType(Long typeId) {
        return typeRepository.findByIdAndDeletedFalse(typeId)
                .orElseThrow(() -> new ServiceException(AndonErrorCodeConstants.TYPE_NOT_EXISTS));
    }

    private AndonTypeEntity requireType(Map<Long, AndonTypeEntity> typesById, Long typeId) {
        AndonTypeEntity andonType = typesById.get(typeId);
        if (andonType == null || Boolean.TRUE.equals(andonType.getDeleted())) {
            throw new ServiceException(AndonErrorCodeConstants.TYPE_NOT_EXISTS);
        }
        return andonType;
    }

    private AndonEventEntity getEventEntity(Long id) {
        return eventRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(AndonErrorCodeConstants.EVENT_NOT_EXISTS));
    }

    private AndonEventEntity getEventForUpdate(Long id) {
        return eventRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(AndonErrorCodeConstants.EVENT_NOT_EXISTS));
    }

    private void saveEvent(AndonEventEntity event) {
        try {
            eventRepository.saveAndFlush(event);
            if (event.getId() != null) {
                andonCache.evictDetailAfterCommit(AndonRedisKeyConstants.EVENT_RESOURCE, event.getId());
            }
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(AndonErrorCodeConstants.EVENT_NO_DUPLICATE);
        }
    }

    private Long getCurrentOperatorId() {
        if (SecurityContextHolder.getLoginUser() == null) {
            return DEFAULT_OPERATOR_ID;
        }
        return SecurityContextHolder.getRequiredLoginUserId();
    }
}
