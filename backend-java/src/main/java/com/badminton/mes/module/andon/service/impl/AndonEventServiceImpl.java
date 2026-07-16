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

/**
 * 现场安灯事件核心业务实现。
 *
 * <p>创建事件时根据类型选择三种处理模式：{@code NO_ACTION} 由系统立即完成并关闭，
 * {@code SELF_HANDLE} 指派给发起人自行确认处理，{@code ASSISTANCE} 则按“产线配置优先、
 * 全局配置其次、类型默认规则兜底”的顺序确定责任主体、响应时限、升级时限和通知渠道。
 *
 * <p>人工操作严格沿状态机推进，行级锁避免并发重复处理；普通处理动作只允许当前指派用户、
 * 指派角色或管理角色执行，最终关闭仅允许管理角色执行。每次有效变更均写入处理日志，并按当前
 * 最佳可用规则生成模拟通知记录。设备灯控同样为状态模拟，不调用外部硬件。
 *
 * <p>超时扫描先查询候选事件，再通过 {@link TransactionTemplate} 为每个事件开启
 * {@code REQUIRES_NEW} 事务。单条事件的锁冲突、规则错误或通知写入失败不会回滚同轮其他事件；
 * 失败记录也使用独立事务，连失败记录本身异常时仍保留下一轮重试能力。
 */
@Service
public class AndonEventServiceImpl implements AndonEventService {

    /** 启用值、全局范围哨兵值及无登录上下文时用于系统动作的默认操作者。 */
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
    /** 可越过普通指派限制处理事件，且可执行最终关闭验收的管理角色。 */
    private static final Set<String> MANAGEMENT_ROLE_CODES = Set.of(
            "ADMIN", "WORKSHOP_MANAGER", "TEAM_LEADER");
    /** 允许作为安灯事件可信业务来源的活动工单状态。 */
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
    /** 为超时候选逐条创建新事务，避免外层只读事务或同批事件之间相互污染。 */
    private final TransactionTemplate timeoutTransactionTemplate;

    /**
     * 注入事件、规则、审计、通知及外部业务引用服务，并配置超时处理的独立事务模板。
     */
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

    /**
     * 创建事件前锁定类型和原因，校验并以质量记录、工单、设备等可信来源补齐业务引用。
     *
     * <p>三种模式共享事件编号、严重度、超时和灯控初始化；仅协助模式生成初始通知，
     * 自处理模式直接指派发起人，无须处理模式在同一事务内自动走完确认、完成和关闭语义。
     */
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

    /** 确认待确认事件，校验实际原因归属与处理权限，并清除响应截止时间。 */
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

    /** 由有权处理人将已确认事件推进到处理中，并同步记录日志和状态通知。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startProcessing(Long id, AndonEventActionReqVO request) {
        AndonEventEntity event = getEventForUpdate(id);
        requireStatus(event, STATUS_CONFIRMED);
        requireCanHandle(event);
        transitionSimple(event, request, STATUS_PROCESSING, "START_PROCESS", "异常开始处理");
    }

    /**
     * 在已确认或处理中阶段变更责任主体；转派说明必填，状态保持不变以保留处理阶段语义。
     */
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

    /**
     * 验证实际原因和处理结果完整后进入待关闭，记录影响数据并终止响应、升级计时。
     */
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

    /** 仅允许管理人员关闭待关闭事件，并将已开启的模拟设备灯切换为关闭。 */
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

    /**
     * 手工升级事件；请求指定的对象优先于规则默认升级对象，升级不改变业务处理状态。
     */
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

    /**
     * 扫描当前时刻的超时候选，并在独立新事务中逐条重读和加锁后处理。
     *
     * <p>候选快照仅提供主键，最终是否到期以新事务中的最新状态为准。单条运行时异常被捕获后
     * 单独记录失败，不中断循环；返回值只统计真正完成响应超时标记或自动升级的事件。
     */
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

    /**
     * 在新的事务中记录单事件超时处理失败，并清除截止时间以避免持续形成失败热点。
     * 失败记录事务再次异常时吞掉异常，使批次继续执行并保留事件原状态供后续扫描重试。
     */
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

    /** 提取可审计的失败信息；无异常消息时退化为异常类型名称。 */
    private String getFailureMessage(RuntimeException exception) {
        String failureMessage = exception.getMessage();
        return StringUtils.hasText(failureMessage) ? failureMessage : exception.getClass().getSimpleName();
    }

    /**
     * 在单事件新事务内重新加锁判断：升级到期优先于响应超时，关闭或尚未到期则不处理。
     */
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

    /**
     * 查询事件聚合详情；缓存未命中时一次装配类型、处理日志和模拟通知记录。
     */
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

    /**
     * 分页返回事件摘要，批量预取类型以避免逐条查询；日志和通知集合刻意留空以控制列表开销。
     */
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

    /** 查询事件处理日志，并先验证事件仍然存在且未被逻辑删除。 */
    @Override
    @Transactional(readOnly = true)
    public List<AndonProcessLogRespVO> getProcessLogs(Long id) {
        getEventEntity(id);
        return processLogRepository.findByEventIdOrderByIdAsc(id).stream()
                .map(AndonEventConvert::toProcessLogRespVO)
                .toList();
    }

    /** 统一执行不含附加字段的状态迁移，保证主表、处理日志和状态通知在同一事务内落库。 */
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

    /**
     * 按事件产线解析协助规则，初始化处理责任和从发起时刻计算的响应、升级截止时间。
     */
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

    /**
     * 将无须处理事件在创建事务中直接闭环，并用发起人和同一时间戳补齐完整审计字段。
     */
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

    /**
     * 校验事件业务引用并从可信来源反向补齐上下文。
     *
     * <p>当前不接受生产任务和工序直接引用；产线必须由已提交质量记录或有效设备证明，工单、设备、
     * 质量记录之间的车间、产线和批次必须一致，防止调用方拼接出不存在的生产现场关系。
     */
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

    /** 对齐请求标识与可信来源标识；两者同时存在但不一致时拒绝创建事件。 */
    private Long reconcileId(Long requestedId, Long trustedId) {
        if (requestedId != null && trustedId != null && !requestedId.equals(trustedId)) {
            throw new ServiceException(AndonErrorCodeConstants.EVENT_REFERENCE_INVALID);
        }
        return trustedId == null ? requestedId : trustedId;
    }

    /** 对齐批次等文本引用，优先采用可信来源中的非空值。 */
    private String reconcileText(String requestedText, String trustedText) {
        if (StringUtils.hasText(requestedText) && StringUtils.hasText(trustedText)
                && !requestedText.equals(trustedText)) {
            throw new ServiceException(AndonErrorCodeConstants.EVENT_REFERENCE_INVALID);
        }
        return StringUtils.hasText(trustedText) ? trustedText : requestedText;
    }

    /**
     * 为新事件解析协助规则并锁定配置：具体产线优先，其次使用全局范围，最后回退到类型默认规则。
     */
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

    /**
     * 为存量事件尽力恢复通知和升级规则。
     *
     * <p>先查最新启用的产线或全局配置；若维护期间配置状态已变化，再从未删除历史配置中选择
     * 最新匹配项，最后尝试类型默认协助规则。该回退链保证事件发起后配置变化不至于切断审计通知。
     */
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

    /** 将持久化配置投影为事件处理所需的不可变规则快照。 */
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

    /**
     * 使用类型上的默认角色、响应时限和通知渠道构造兜底规则；缺少任一必需项即视为未匹配配置。
     */
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

    /** 判断事件是否存在已到期且尚未执行过的升级截止时间。 */
    private boolean shouldEscalate(AndonEventEntity event, LocalDateTime now) {
        return !TIMEOUT_ESCALATED.equals(event.getTimeoutStatus())
                && event.getEscalationDeadline() != null
                && !event.getEscalationDeadline().isAfter(now);
    }

    /** 响应超时只适用于仍待确认、超时状态正常且响应截止时间已到的事件。 */
    private boolean shouldMarkResponseOverdue(AndonEventEntity event, LocalDateTime now) {
        return STATUS_PENDING_CONFIRMATION.equals(event.getEventStatus())
                && TIMEOUT_NORMAL.equals(event.getTimeoutStatus())
                && event.getResponseDeadline() != null
                && !event.getResponseDeadline().isAfter(now);
    }

    /**
     * 自动升级到规则中的升级责任主体并记录系统日志、模拟通知。
     * 若存量规则没有升级对象，则清除升级截止时间并返回未处理，避免每轮重复命中同一无效规则。
     */
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

    /** 校验实际原因存在、启用且归属于事件类型。 */
    private void validateReason(Long reasonId, Long typeId) {
        validateReasonForUpdate(reasonId, typeId);
    }

    /**
     * 以行锁读取可选原因，防止事件创建或确认期间原因被并发禁用、删除或调整归属。
     */
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

    /**
     * 验证责任主体至少包含有效用户或有效角色；两者同时提供时分别校验，支持联合指派。
     */
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

    /** 要求处理结果和实际原因完整；请求未重复提供原因时沿用确认阶段已归属的原因。 */
    private void validateCompletionRequest(AndonEventEntity event, AndonEventActionReqVO request) {
        Long actualReasonId = request.getActualReasonId() == null
                ? event.getActualReasonId() : request.getActualReasonId();
        if (actualReasonId == null || !StringUtils.hasText(request.getProcessingResult())) {
            throw new ServiceException(AndonErrorCodeConstants.EVENT_RESULT_INCOMPLETE);
        }
        validateReason(actualReasonId, event.getAndonTypeId());
    }

    /**
     * 初始化模拟灯控状态：类型未启用灯控则无须操作，缺少设备则标记失败，否则模拟开灯成功。
     */
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

    /** 按存量事件的最佳可用规则，为当前责任主体生成状态类模拟通知。 */
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

    /** 事件处理期间使用的责任、时限与通知渠道规则快照。 */
    private record AssistanceRule(
            Long handlerUserId,
            String handlerRoleCode,
            Long escalationUserId,
            String escalationRoleCode,
            Integer responseMinutes,
            Integer escalationMinutes,
            String notificationChannels) {
    }

    /**
     * 将逗号分隔渠道逐项落为模拟发送记录；当前只保留发送审计，不接入真实消息网关。
     */
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

    /** 构造一条已模拟发送的通知记录，保留接收用户、角色、渠道和发送时刻。 */
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

    /**
     * 记录事件动作、状态前后值、操作者和目标责任主体，形成独立于主表当前值的审计轨迹。
     */
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

    /** 组合毫秒时间戳与随机片段生成事件业务编号，数据库唯一约束负责最终并发兜底。 */
    private String generateEventNo(LocalDateTime now) {
        return "AND" + EVENT_TIME_FORMATTER.format(now)
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    /** 强制事件处于唯一允许的状态，阻止跳步或重复状态迁移。 */
    private void requireStatus(AndonEventEntity event, String requiredStatus) {
        if (!requiredStatus.equals(event.getEventStatus())) {
            throw new ServiceException(AndonErrorCodeConstants.EVENT_STATUS_INVALID);
        }
    }

    /** 强制事件处于给定状态集合之一。 */
    private void requireAnyStatus(AndonEventEntity event, String... allowedStatuses) {
        if (!Set.of(allowedStatuses).contains(event.getEventStatus())) {
            throw new ServiceException(AndonErrorCodeConstants.EVENT_STATUS_INVALID);
        }
    }

    /**
     * 校验普通处理权限：无登录上下文视为系统调用；管理角色、指派用户或拥有指派角色者可操作。
     */
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

    /** 最终关闭要求系统调用或管理角色，普通责任人只能将事件处理到待关闭。 */
    private void requireManagementOperator() {
        LoginUser loginUser = SecurityContextHolder.getLoginUser();
        if (loginUser == null || hasManagementRole(loginUser)) {
            return;
        }
        throw new ServiceException(AndonErrorCodeConstants.EVENT_OPERATION_FORBIDDEN);
    }

    /** 判断登录用户是否拥有任一安灯管理角色。 */
    private boolean hasManagementRole(LoginUser loginUser) {
        return loginUser.getRoleCodes() != null
                && loginUser.getRoleCodes().stream().anyMatch(MANAGEMENT_ROLE_CODES::contains);
    }

    /** 读取未删除且启用的安灯类型。 */
    private AndonTypeEntity getEnabledType(Long typeId) {
        AndonTypeEntity andonType = getType(typeId);
        if (!Integer.valueOf(ENABLED).equals(andonType.getEnabledStatus())) {
            throw new ServiceException(AndonErrorCodeConstants.TYPE_NOT_EXISTS);
        }
        return andonType;
    }

    /** 加锁读取未删除且启用的类型，稳定事件创建期间的处理模式和默认规则。 */
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

    /** 从分页预取结果中取得有效类型，缺失关联数据时拒绝返回不完整响应。 */
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

    /** 以行锁读取事件，串行化同一事件的状态迁移和责任变更。 */
    private AndonEventEntity getEventForUpdate(Long id) {
        return eventRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(AndonErrorCodeConstants.EVENT_NOT_EXISTS));
    }

    /**
     * 立即刷新事件以尽早暴露编号唯一约束，并在事务提交后失效事件详情缓存，避免读到未提交状态。
     */
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

    /** 返回当前登录用户；调度和其他无登录上下文调用使用系统默认操作者。 */
    private Long getCurrentOperatorId() {
        if (SecurityContextHolder.getLoginUser() == null) {
            return DEFAULT_OPERATOR_ID;
        }
        return SecurityContextHolder.getRequiredLoginUserId();
    }
}
