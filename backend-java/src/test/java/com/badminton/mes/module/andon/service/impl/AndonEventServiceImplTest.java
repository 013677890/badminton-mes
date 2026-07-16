package com.badminton.mes.module.andon.service.impl;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.andon.constants.AndonErrorCodeConstants;
import com.badminton.mes.module.andon.controller.vo.AndonEventActionReqVO;
import com.badminton.mes.module.andon.dal.entity.AndonEventEntity;
import com.badminton.mes.module.andon.dal.entity.AndonProcessLogEntity;
import com.badminton.mes.module.andon.dal.entity.AndonReasonEntity;
import com.badminton.mes.module.andon.dal.redis.AndonCache;
import com.badminton.mes.module.andon.dal.redis.AndonRedisKeyConstants;
import com.badminton.mes.module.andon.dal.repository.AndonConfigurationRepository;
import com.badminton.mes.module.andon.dal.repository.AndonEventRepository;
import com.badminton.mes.module.andon.dal.repository.AndonNotificationRecordRepository;
import com.badminton.mes.module.andon.dal.repository.AndonProcessLogRepository;
import com.badminton.mes.module.andon.dal.repository.AndonReasonRepository;
import com.badminton.mes.module.andon.dal.repository.AndonTypeRepository;
import com.badminton.mes.module.equipment.service.EquipmentLedgerService;
import com.badminton.mes.module.production.service.WorkOrderService;
import com.badminton.mes.module.production.service.WorkshopService;
import com.badminton.mes.module.quality.service.QualityInspectionRecordService;
import com.badminton.mes.module.system.service.RoleService;
import com.badminton.mes.module.system.service.UserService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AndonEventServiceImpl} 单元测试。
 *
 * <p>被测主状态机覆盖“已确认 -> 处理中 -> 待关闭 -> 已关闭”的关键流转，同时验证非法前置状态、
 * 处理结果完整性以及指派人/管理角色权限边界。仓储、跨模块服务、事务管理器和缓存均由 Mock 隔离，
 * 使测试只关注事件实体变更、处理日志、灯控结果以及调用缓存组件安排事务后失效等服务层副作用。
 */
@ExtendWith(MockitoExtension.class)
class AndonEventServiceImplTest {

    /** 各用例共用的事件主键，便于关联行锁查询、持久化和缓存键断言。 */
    private static final Long EVENT_ID = 100L;

    /** 事件所属安灯类型，用于校验实际原因与事件类型的一致性。 */
    private static final Long ANDON_TYPE_ID = 200L;

    /** 完成事件时选用的实际原因主键。 */
    private static final Long REASON_ID = 300L;

    /** 默认指派处理人，同时作为正常处理流程中的登录用户。 */
    private static final Long ASSIGNED_USER_ID = 400L;

    /** 隔离事件加锁读取与状态持久化，便于观察状态机写入边界。 */
    @Mock
    private AndonEventRepository eventRepository;

    /** 隔离流程日志落库，并通过 Captor 核对审计内容。 */
    @Mock
    private AndonProcessLogRepository processLogRepository;

    /** 隔离通知记录持久化，避免通知链路参与事件状态测试。 */
    @Mock
    private AndonNotificationRecordRepository notificationRepository;

    /** 隔离安灯类型查询，使事件测试不依赖类型数据源。 */
    @Mock
    private AndonTypeRepository typeRepository;

    /** 隔离原因查询，用于精确构造完成事件所需的有效原因。 */
    @Mock
    private AndonReasonRepository reasonRepository;

    /** 隔离处理规则查询，避免配置数据影响状态迁移断言。 */
    @Mock
    private AndonConfigurationRepository configurationRepository;

    /** 以下跨模块服务均被隔离，防止工单、车间、设备和质检查询扩散为集成测试。 */
    @Mock
    private WorkOrderService workOrderService;

    @Mock
    private WorkshopService workshopService;

    @Mock
    private EquipmentLedgerService equipmentLedgerService;

    @Mock
    private QualityInspectionRecordService qualityRecordService;

    /** 隔离用户资料查询，权限结论仅由当前用例构造的身份决定。 */
    @Mock
    private UserService userService;

    /** 隔离角色服务，专注验证事件服务自身的角色判定分支。 */
    @Mock
    private RoleService roleService;

    /** 记录事务提交后详情缓存应失效的外部副作用。 */
    @Mock
    private AndonCache andonCache;

    /** 隔离事务同步机制，避免单元测试启动真实事务基础设施。 */
    @Mock
    private PlatformTransactionManager transactionManager;

    /** 注入全部 Mock 后直接执行的被测服务实例。 */
    private AndonEventServiceImpl eventService;

    @BeforeEach
    void setUp() {
        // 显式组装依赖，保证每个用例都从无共享调用记录的服务实例开始。
        eventService = new AndonEventServiceImpl(
                eventRepository,
                processLogRepository,
                notificationRepository,
                typeRepository,
                reasonRepository,
                configurationRepository,
                workOrderService,
                workshopService,
                equipmentLedgerService,
                qualityRecordService,
                userService,
                roleService,
                andonCache,
                transactionManager);
    }

    @AfterEach
    void tearDown() {
        // 登录身份存放在线程上下文中，必须清理以免权限状态泄漏到后续用例。
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("开始处理安灯事件：已确认事件进入处理中并记录日志")
    void startProcessingMovesConfirmedEventToProcessing() {
        // 以事件指派人的身份进入权限分支，排除角色兜底授权对结果的干扰。
        setLoginUser(ASSIGNED_USER_ID, List.of("OPERATOR"));
        AndonEventEntity event = buildEvent("CONFIRMED");
        // 加锁查询固定返回同一实体，以便直接观察服务方法对受管状态的修改。
        when(eventRepository.findByIdAndDeletedFalseForUpdate(EVENT_ID))
                .thenReturn(Optional.of(event));
        AndonEventActionReqVO request = new AndonEventActionReqVO();
        request.setActionContent("开始排查设备异常");

        eventService.startProcessing(EVENT_ID, request);

        assertThat(event.getEventStatus()).isEqualTo("PROCESSING");
        verify(eventRepository).saveAndFlush(event);
        // 状态写入后必须安排清除事件详情，避免提交后仍读到旧状态。
        verify(andonCache).evictDetailAfterCommit(
                AndonRedisKeyConstants.EVENT_RESOURCE,
                EVENT_ID);
        // 捕获实际落库日志，联合校验动作、状态边和操作人，而非只验证发生过写入。
        ArgumentCaptor<AndonProcessLogEntity> logCaptor =
                ArgumentCaptor.forClass(AndonProcessLogEntity.class);
        verify(processLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo("START_PROCESS");
        assertThat(logCaptor.getValue().getFromStatus()).isEqualTo("CONFIRMED");
        assertThat(logCaptor.getValue().getToStatus()).isEqualTo("PROCESSING");
        assertThat(logCaptor.getValue().getOperatorId()).isEqualTo(ASSIGNED_USER_ID);
    }

    @Test
    @DisplayName("开始处理安灯事件：未确认状态不允许开始处理")
    void startProcessingRejectsInvalidStatus() {
        // 身份保持合法，使异常能够明确归因于状态机前置状态不满足。
        setLoginUser(ASSIGNED_USER_ID, List.of("OPERATOR"));
        when(eventRepository.findByIdAndDeletedFalseForUpdate(EVENT_ID))
                .thenReturn(Optional.of(buildEvent("PENDING_CONFIRMATION")));

        // 拒绝路径还需证明没有产生实体写入或流程日志，保证失败操作无部分副作用。
        assertThatThrownBy(() -> eventService.startProcessing(
                EVENT_ID, new AndonEventActionReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                AndonErrorCodeConstants.EVENT_STATUS_INVALID));
        verify(eventRepository, never()).saveAndFlush(any());
        verify(processLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("开始处理安灯事件：非指派人且无指派角色时拒绝操作")
    void startProcessingRejectsUnauthorizedOperator() {
        // 使用普通角色的其他用户，刻意同时绕开“本人”和“指派角色”两条授权路径。
        setLoginUser(999L, List.of("INSPECTOR"));
        when(eventRepository.findByIdAndDeletedFalseForUpdate(EVENT_ID))
                .thenReturn(Optional.of(buildEvent("CONFIRMED")));

        // 权限拒绝必须发生在持久化之前，防止未授权用户推进事件状态。
        assertThatThrownBy(() -> eventService.startProcessing(
                EVENT_ID, new AndonEventActionReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                AndonErrorCodeConstants.EVENT_OPERATION_FORBIDDEN));
        verify(eventRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("完成安灯事件：填写原因和结果后进入待关闭状态")
    void completeEventMovesProcessingEventToWaitingClose() {
        // 完成动作仍由指派人执行，确保用例重点落在结果校验和状态迁移。
        setLoginUser(ASSIGNED_USER_ID, List.of("OPERATOR"));
        AndonEventEntity event = buildEvent("PROCESSING");
        // 构造同类型、已启用且未删除的原因，满足原因有效性的全部业务前置条件。
        AndonReasonEntity reason = new AndonReasonEntity();
        reason.setId(REASON_ID);
        reason.setAndonTypeId(ANDON_TYPE_ID);
        reason.setEnabledStatus(1);
        reason.setDeleted(false);
        when(eventRepository.findByIdAndDeletedFalseForUpdate(EVENT_ID))
                .thenReturn(Optional.of(event));
        when(reasonRepository.findByIdAndDeletedFalseForUpdate(REASON_ID))
                .thenReturn(Optional.of(reason));
        AndonEventActionReqVO request = new AndonEventActionReqVO();
        request.setActualReasonId(REASON_ID);
        request.setProcessingResult("已更换损坏传感器");
        request.setImpactMinutes(15);
        request.setAffectedQuantity(20);
        request.setActionContent("异常处理完成");

        eventService.completeEvent(EVENT_ID, request);

        // 除目标状态外，同时核对处理结果、完成人和超时截止字段清理等聚合内副作用。
        assertThat(event.getEventStatus()).isEqualTo("WAITING_CLOSE");
        assertThat(event.getActualReasonId()).isEqualTo(REASON_ID);
        assertThat(event.getProcessingResult()).isEqualTo("已更换损坏传感器");
        assertThat(event.getCompletedBy()).isEqualTo(ASSIGNED_USER_ID);
        assertThat(event.getCompletedAt()).isNotNull();
        assertThat(event.getResponseDeadline()).isNull();
        assertThat(event.getEscalationDeadline()).isNull();
        verify(eventRepository).saveAndFlush(event);
        // 完成后的详情缓存必须在事务提交后失效，避免查询端保留处理中快照。
        verify(andonCache).evictDetailAfterCommit(
                AndonRedisKeyConstants.EVENT_RESOURCE,
                EVENT_ID);
    }

    @Test
    @DisplayName("完成安灯事件：缺少实际原因或处理结果时拒绝完成")
    void completeEventRejectsIncompleteResult() {
        // 登录和状态均合法，仅留下“缺少实际原因”这一项输入缺陷。
        setLoginUser(ASSIGNED_USER_ID, List.of("OPERATOR"));
        when(eventRepository.findByIdAndDeletedFalseForUpdate(EVENT_ID))
                .thenReturn(Optional.of(buildEvent("PROCESSING")));
        AndonEventActionReqVO request = new AndonEventActionReqVO();
        request.setProcessingResult("已处理");

        // 校验失败不得留下状态保存或成功流程日志，避免形成不可追溯的半完成事件。
        assertThatThrownBy(() -> eventService.completeEvent(EVENT_ID, request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                AndonErrorCodeConstants.EVENT_RESULT_INCOMPLETE));
        verify(eventRepository, never()).saveAndFlush(any());
        verify(processLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("关闭安灯事件：管理角色关闭事件并关闭设备灯控")
    void closeEventClosesLightForManagementOperator() {
        // 关闭操作使用车间管理角色，覆盖区别于指派处理人的管理权限通道。
        setLoginUser(500L, List.of("WORKSHOP_MANAGER"));
        AndonEventEntity event = buildEvent("WAITING_CLOSE");
        event.setLightStatus("ON");
        when(eventRepository.findByIdAndDeletedFalseForUpdate(EVENT_ID))
                .thenReturn(Optional.of(event));
        AndonEventActionReqVO request = new AndonEventActionReqVO();
        request.setActionContent("主管确认关闭");

        eventService.closeEvent(EVENT_ID, request);

        // 同时验证事件终态、关闭审计字段及灯控回执，确保业务闭环完整。
        assertThat(event.getEventStatus()).isEqualTo("CLOSED");
        assertThat(event.getClosedBy()).isEqualTo(500L);
        assertThat(event.getClosedAt()).isNotNull();
        assertThat(event.getLightStatus()).isEqualTo("OFF");
        assertThat(event.getLightMessage()).contains("关闭设备安灯成功");
        verify(eventRepository).saveAndFlush(event);
        // 灯控与状态均已改变，提交后需淘汰详情缓存中的旧聚合视图。
        verify(andonCache).evictDetailAfterCommit(
                AndonRedisKeyConstants.EVENT_RESOURCE,
                EVENT_ID);
    }

    @Test
    @DisplayName("关闭安灯事件：普通处理人无管理角色时拒绝关闭")
    void closeEventRejectsNonManagementOperator() {
        // 即使当前用户是事件指派人，缺少管理角色仍不能执行最终关闭。
        setLoginUser(ASSIGNED_USER_ID, List.of("OPERATOR"));
        when(eventRepository.findByIdAndDeletedFalseForUpdate(EVENT_ID))
                .thenReturn(Optional.of(buildEvent("WAITING_CLOSE")));

        // 权限异常之后禁止保存，证明服务没有仅凭指派关系绕过关闭权限。
        assertThatThrownBy(() -> eventService.closeEvent(
                EVENT_ID, new AndonEventActionReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                AndonErrorCodeConstants.EVENT_OPERATION_FORBIDDEN));
        verify(eventRepository, never()).saveAndFlush(any());
    }

    /**
     * 构造满足通用业务约束的事件，仅开放状态参数给各用例切换状态机起点。
     */
    private AndonEventEntity buildEvent(String eventStatus) {
        AndonEventEntity event = new AndonEventEntity();
        event.setId(EVENT_ID);
        event.setEventNo("AND-001");
        event.setAndonTypeId(ANDON_TYPE_ID);
        event.setEventStatus(eventStatus);
        event.setAssignedUserId(ASSIGNED_USER_ID);
        event.setAssignedRoleCode("OPERATOR");
        event.setTimeoutStatus("NORMAL");
        event.setLightStatus("NOT_REQUIRED");
        event.setDeleted(false);
        return event;
    }

    /**
     * 写入最小登录上下文，用于稳定复现指派人、普通角色和管理角色的权限分支。
     */
    private void setLoginUser(Long userId, List<String> roleCodes) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);
        loginUser.setRoleCodes(roleCodes);
        SecurityContextHolder.set("unit-test-token", loginUser);
    }
}
