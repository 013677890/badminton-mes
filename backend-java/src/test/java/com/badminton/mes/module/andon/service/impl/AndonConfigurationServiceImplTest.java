package com.badminton.mes.module.andon.service.impl;

import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.andon.constants.AndonErrorCodeConstants;
import com.badminton.mes.module.andon.controller.vo.AndonConfigurationSaveReqVO;
import com.badminton.mes.module.andon.dal.entity.AndonConfigurationEntity;
import com.badminton.mes.module.andon.dal.entity.AndonTypeEntity;
import com.badminton.mes.module.andon.dal.redis.AndonCache;
import com.badminton.mes.module.andon.dal.redis.AndonRedisKeyConstants;
import com.badminton.mes.module.andon.dal.repository.AndonConfigurationRepository;
import com.badminton.mes.module.andon.dal.repository.AndonTypeRepository;
import com.badminton.mes.module.system.controller.vo.UserRespVO;
import com.badminton.mes.module.system.service.RoleService;
import com.badminton.mes.module.system.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AndonConfigurationServiceImpl} 单元测试。
 *
 * <p>被测配置生命周期涵盖创建、受活动事件保护的更新，以及“启用 -> 停用并逻辑删除”的删除状态变化。
 * 仓储、用户/角色服务和缓存均由 Mock 隔离，覆盖全局范围归一化、默认值、处理人与升级规则、
 * 同类型同范围唯一性、引用保护，以及服务是否调用缓存组件安排事务提交后详情失效等关键行为。
 */
@ExtendWith(MockitoExtension.class)
class AndonConfigurationServiceImplTest {

    /** 配置主键，用于更新、删除及详情缓存键的关联断言。 */
    private static final Long CONFIGURATION_ID = 100L;

    /** 配置绑定的安灯类型主键，用于类型可用性和活动事件校验。 */
    private static final Long ANDON_TYPE_ID = 200L;

    /** 默认处理人主键，用于构造满足处理规则的有效配置。 */
    private static final Long HANDLER_USER_ID = 300L;

    /** 隔离配置加锁读取、唯一性查询和持久化操作。 */
    @Mock
    private AndonConfigurationRepository configurationRepository;

    /** 隔离安灯类型查询，使配置规则测试不依赖真实类型数据。 */
    @Mock
    private AndonTypeRepository typeRepository;

    /** 隔离处理人状态查询，用于精确控制用户是否可被配置。 */
    @Mock
    private UserService userService;

    /** 隔离角色有效性查询，避免配置单元测试扩展为系统模块集成测试。 */
    @Mock
    private RoleService roleService;

    /** 记录配置变更在事务提交后触发的详情缓存失效。 */
    @Mock
    private AndonCache andonCache;

    /** 注入全部 Mock 后直接执行的被测服务实例。 */
    private AndonConfigurationServiceImpl configurationService;

    @BeforeEach
    void setUp() {
        // 每个用例重新组装服务，隔离 Mockito 调用记录和服务实例状态。
        configurationService = new AndonConfigurationServiceImpl(
                configurationRepository,
                typeRepository,
                userService,
                roleService,
                andonCache);
    }

    @Test
    @DisplayName("创建安灯配置：无产线时使用全局范围并默认启用")
    void createConfigurationUsesGlobalScopeAndDefaultsEnabled() {
        // 从完整请求中移除范围和启用值，专门触发全局范围及默认启用的归一化逻辑。
        AndonConfigurationSaveReqVO request = buildValidRequest();
        request.setProductionLineId(null);
        request.setEnabledStatus(null);
        stubAvailableTypeAndHandler();
        // 模拟数据库生成主键，同时返回服务实际提交的同一实体供后续结果链路使用。
        when(configurationRepository.saveAndFlush(any(AndonConfigurationEntity.class)))
                .thenAnswer(invocation -> {
                    AndonConfigurationEntity configuration = invocation.getArgument(0);
                    configuration.setId(CONFIGURATION_ID);
                    return configuration;
                });

        Long createdId = configurationService.createConfiguration(request);

        assertThat(createdId).isEqualTo(CONFIGURATION_ID);
        // 捕获持久化实体，验证请求归一化、审计默认值和逻辑删除初始值均已落入模型。
        ArgumentCaptor<AndonConfigurationEntity> configurationCaptor =
                ArgumentCaptor.forClass(AndonConfigurationEntity.class);
        verify(configurationRepository).saveAndFlush(configurationCaptor.capture());
        AndonConfigurationEntity savedConfiguration = configurationCaptor.getValue();
        assertThat(savedConfiguration.getScopeLineId()).isZero();
        assertThat(savedConfiguration.getEnabledStatus()).isEqualTo(1);
        assertThat(savedConfiguration.getCreateBy()).isEqualTo(1L);
        assertThat(savedConfiguration.getDeleted()).isFalse();
    }

    @Test
    @DisplayName("创建安灯配置：处理人和处理角色均为空时拒绝创建")
    void createConfigurationRejectsMissingHandler() {
        // 保持类型有效，仅清空两种处理主体，使失败原因唯一落在处理规则不完整。
        AndonConfigurationSaveReqVO request = buildValidRequest();
        request.setHandlerUserId(null);
        request.setHandlerRoleCode(null);
        when(typeRepository.findByIdAndDeletedFalseForUpdate(ANDON_TYPE_ID))
                .thenReturn(Optional.of(buildType()));

        // 规则校验失败必须先于保存发生，避免产生无人负责的配置记录。
        assertThatThrownBy(() -> configurationService.createConfiguration(request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                AndonErrorCodeConstants.CONFIGURATION_RULE_INVALID));
        verify(configurationRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("创建安灯配置：升级时限不大于响应时限时拒绝创建")
    void createConfigurationRejectsInvalidEscalationDeadline() {
        // 响应时限已由辅助方法设为十分钟，此处构造相等的升级时限以命中严格递增约束。
        AndonConfigurationSaveReqVO request = buildValidRequest();
        request.setEscalationMinutes(10);
        request.setEscalationUserId(301L);
        stubAvailableTypeAndHandler();

        // 用户与类型前置条件均可用，确保异常明确来自时限关系而非外部依赖。
        assertThatThrownBy(() -> configurationService.createConfiguration(request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                AndonErrorCodeConstants.CONFIGURATION_RULE_INVALID));
        verify(configurationRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("创建安灯配置：同类型同范围已存在配置时拒绝创建")
    void createConfigurationRejectsDuplicateScope() {
        AndonConfigurationSaveReqVO request = buildValidRequest();
        stubAvailableTypeAndHandler();
        // 精确模拟同一类型、同一产线已有未删除配置，覆盖业务唯一键冲突分支。
        when(configurationRepository.existsByAndonTypeIdAndScopeLineIdAndDeletedFalse(
                ANDON_TYPE_ID, 10L)).thenReturn(true);

        // 冲突只应返回领域错误，不得覆盖或追加任何配置。
        assertThatThrownBy(() -> configurationService.createConfiguration(request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                AndonErrorCodeConstants.CONFIGURATION_SCOPE_DUPLICATE));
        verify(configurationRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("更新安灯配置：存在活动事件时拒绝修改")
    void updateConfigurationRejectsActiveEvents() {
        AndonConfigurationEntity configuration = buildConfiguration();
        AndonConfigurationSaveReqVO request = buildValidRequest();
        // 同时桩化普通读取、类型锁和配置锁，完整通过更新入口直到活动事件保护检查。
        when(configurationRepository.findByIdAndDeletedFalse(CONFIGURATION_ID))
                .thenReturn(Optional.of(configuration));
        when(typeRepository.findByIdAndDeletedFalseForUpdate(ANDON_TYPE_ID))
                .thenReturn(Optional.of(buildType()));
        when(configurationRepository.findByIdAndDeletedFalseForUpdate(CONFIGURATION_ID))
                .thenReturn(Optional.of(configuration));
        when(configurationRepository.countActiveEventsByAndonTypeId(ANDON_TYPE_ID))
                .thenReturn(1L);

        // 活动事件期间配置不可漂移，既不能落库，也不能误发缓存失效信号。
        assertThatThrownBy(() -> configurationService.updateConfiguration(
                CONFIGURATION_ID, request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                AndonErrorCodeConstants.CONFIGURATION_HAS_ACTIVE_EVENTS));
        verify(configurationRepository, never()).saveAndFlush(any());
        verify(andonCache, never()).evictDetailAfterCommit(any(), any());
    }

    @Test
    @DisplayName("删除安灯配置：改写范围并失效详情缓存")
    void deleteConfigurationRewritesScopeAndEvictsCache() {
        AndonConfigurationEntity configuration = buildConfiguration();
        // 普通读取与加锁读取返回同一对象，便于观察删除流程对聚合的原位修改。
        when(configurationRepository.findByIdAndDeletedFalse(CONFIGURATION_ID))
                .thenReturn(Optional.of(configuration));
        when(typeRepository.findByIdAndDeletedFalseForUpdate(ANDON_TYPE_ID))
                .thenReturn(Optional.of(buildType()));
        when(configurationRepository.findByIdAndDeletedFalseForUpdate(CONFIGURATION_ID))
                .thenReturn(Optional.of(configuration));

        configurationService.deleteConfiguration(CONFIGURATION_ID);

        // 删除不仅设置逻辑标记，还释放原范围唯一键，允许后续重新创建同范围配置。
        assertThat(configuration.getScopeLineId()).isEqualTo(-CONFIGURATION_ID);
        assertThat(configuration.getEnabledStatus()).isZero();
        assertThat(configuration.getDeleted()).isTrue();
        verify(configurationRepository).saveAndFlush(configuration);
        // 提交后清除详情缓存，防止逻辑删除记录继续从缓存对外可见。
        verify(andonCache).evictDetailAfterCommit(
                AndonRedisKeyConstants.CONFIGURATION_RESOURCE,
                CONFIGURATION_ID);
    }

    /**
     * 统一桩化可用类型和启用处理人，让创建类用例仅改变各自关注的输入规则。
     */
    private void stubAvailableTypeAndHandler() {
        when(typeRepository.findByIdAndDeletedFalseForUpdate(ANDON_TYPE_ID))
                .thenReturn(Optional.of(buildType()));
        // 用户响应只填充规则校验必需字段，避免无关资料掩盖测试意图。
        UserRespVO user = new UserRespVO();
        user.setId(HANDLER_USER_ID);
        user.setStatus(1);
        when(userService.getUser(HANDLER_USER_ID)).thenReturn(user);
    }

    /**
     * 构造最小有效保存请求，作为各异常用例逐项破坏业务约束的稳定基线。
     */
    private AndonConfigurationSaveReqVO buildValidRequest() {
        AndonConfigurationSaveReqVO request = new AndonConfigurationSaveReqVO();
        request.setAndonTypeId(ANDON_TYPE_ID);
        request.setProductionLineId(10L);
        request.setHandlerUserId(HANDLER_USER_ID);
        request.setResponseMinutes(10);
        request.setNotificationChannels("IN_APP");
        return request;
    }

    /**
     * 构造已启用且未删除的现有配置，用于更新保护和逻辑删除场景。
     */
    private AndonConfigurationEntity buildConfiguration() {
        AndonConfigurationEntity configuration = new AndonConfigurationEntity();
        configuration.setId(CONFIGURATION_ID);
        configuration.setAndonTypeId(ANDON_TYPE_ID);
        configuration.setProductionLineId(10L);
        configuration.setScopeLineId(10L);
        configuration.setHandlerUserId(HANDLER_USER_ID);
        configuration.setResponseMinutes(10);
        configuration.setNotificationChannels("IN_APP");
        configuration.setEnabledStatus(1);
        configuration.setDeleted(false);
        return configuration;
    }

    /**
     * 构造可被配置引用的有效安灯类型，隔离类型状态校验之外的字段。
     */
    private AndonTypeEntity buildType() {
        AndonTypeEntity andonType = new AndonTypeEntity();
        andonType.setId(ANDON_TYPE_ID);
        andonType.setEnabledStatus(1);
        andonType.setDeleted(false);
        return andonType;
    }
}
