package com.badminton.mes.module.andon.service.impl;

import java.util.Optional;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.andon.constants.AndonErrorCodeConstants;
import com.badminton.mes.module.andon.controller.vo.AndonTypePageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonTypeSaveReqVO;
import com.badminton.mes.module.andon.dal.entity.AndonTypeEntity;
import com.badminton.mes.module.andon.dal.redis.AndonCache;
import com.badminton.mes.module.andon.dal.redis.AndonRedisKeyConstants;
import com.badminton.mes.module.andon.dal.repository.AndonConfigurationRepository;
import com.badminton.mes.module.andon.dal.repository.AndonEventRepository;
import com.badminton.mes.module.andon.dal.repository.AndonReasonRepository;
import com.badminton.mes.module.andon.dal.repository.AndonTypeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AndonTypeServiceImpl} 单元测试。
 *
 * <p>被测类型生命周期覆盖创建、更新，以及“启用 -> 停用并逻辑删除”的终态变化；删除时还会改写编码
 * 以释放业务唯一键。类型、原因、配置和事件仓储及缓存均由 Mock 隔离，重点验证默认规则、引用保护、
 * 关联详情缓存级联失效、删除编码防冲突和空分页短路等服务层行为。
 */
@ExtendWith(MockitoExtension.class)
class AndonTypeServiceImplTest {

    /** 各生命周期用例共用的类型主键，也是删除保留编码和缓存键的来源。 */
    private static final Long TYPE_ID = 400L;

    /** 隔离类型查询、唯一性判断和持久化，作为被测聚合的主要数据边界。 */
    @Mock
    private AndonTypeRepository typeRepository;

    /** 隔离原因引用查询，并提供更新时需要级联清理的详情主键。 */
    @Mock
    private AndonReasonRepository reasonRepository;

    /** 隔离配置引用查询，覆盖删除保护及关联缓存清理。 */
    @Mock
    private AndonConfigurationRepository configurationRepository;

    /** 隔离历史事件引用查询，避免类型测试依赖真实事件数据。 */
    @Mock
    private AndonEventRepository eventRepository;

    /** 记录类型及其关联原因、配置、事件的事务后缓存副作用。 */
    @Mock
    private AndonCache andonCache;

    /** 注入全部 Mock 后直接执行的被测服务实例。 */
    private AndonTypeServiceImpl typeService;

    @BeforeEach
    void setUp() {
        // 每个用例使用新服务实例，确保 Mock 调用记录和测试状态互不污染。
        typeService = new AndonTypeServiceImpl(
                typeRepository,
                reasonRepository,
                configurationRepository,
                eventRepository,
                andonCache);
    }

    @Test
    @DisplayName("创建协助处理类型：缺少默认处理规则时拒绝创建")
    void createAssistanceTypeRejectsIncompleteDefaultRule() {
        // 保留协助处理模式，仅移除责任角色以精准破坏该模式要求的默认处理规则。
        AndonTypeSaveReqVO request = buildAssistanceTypeRequest();
        request.setResponsibleRoleCode(null);

        // 规则错误应在持久化之前暴露，避免保存无法自动分派的安灯类型。
        assertThatThrownBy(() -> typeService.createType(request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                AndonErrorCodeConstants.TYPE_RULE_INVALID));
        verify(typeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("创建安灯类型：默认灯控关闭并返回主键")
    void createTypeDefaultsLightControlAndReturnsId() {
        // 主动留空灯控开关，使服务负责填充安全的关闭默认值。
        AndonTypeSaveReqVO request = buildAssistanceTypeRequest();
        request.setLightControlEnabled(null);
        when(typeRepository.existsByTypeCodeAndDeletedFalse(request.getTypeCode()))
                .thenReturn(false);
        // 模拟数据库主键回填，验证服务返回值来自实际持久化实体而非请求数据。
        when(typeRepository.saveAndFlush(any(AndonTypeEntity.class)))
                .thenAnswer(invocation -> {
                    AndonTypeEntity type = invocation.getArgument(0);
                    type.setId(TYPE_ID);
                    return type;
                });

        Long typeId = typeService.createType(request);

        assertThat(typeId).isEqualTo(TYPE_ID);
        verify(typeRepository).saveAndFlush(any(AndonTypeEntity.class));
    }

    @Test
    @DisplayName("更新安灯类型：清除类型及关联详情缓存")
    void updateTypeEvictsTypeAndRelatedDetailCaches() {
        AndonTypeEntity type = buildTypeEntity();
        AndonTypeSaveReqVO request = buildAssistanceTypeRequest();
        // 锁定现有类型并排除编码冲突，让用例进入更新成功后的级联缓存处理阶段。
        when(typeRepository.findByIdAndDeletedFalseForUpdate(TYPE_ID))
                .thenReturn(Optional.of(type));
        when(typeRepository.existsByTypeCodeAndIdNotAndDeletedFalse(request.getTypeCode(), TYPE_ID))
                .thenReturn(false);
        when(reasonRepository.findIdsByAndonTypeIdAndDeletedFalse(TYPE_ID))
                .thenReturn(java.util.List.of(501L));
        when(configurationRepository.findIdsByAndonTypeIdAndDeletedFalse(TYPE_ID))
                .thenReturn(java.util.List.of(502L));
        when(eventRepository.findIdsByAndonTypeIdAndDeletedFalse(TYPE_ID))
                .thenReturn(java.util.List.of(503L));

        typeService.updateType(TYPE_ID, request);

        // 类型规则变化会影响四类详情视图，必须在提交后按资源维度逐一失效。
        verify(andonCache).evictDetailAfterCommit(AndonRedisKeyConstants.TYPE_RESOURCE, TYPE_ID);
        verify(andonCache).evictDetailsAfterCommit(
                AndonRedisKeyConstants.REASON_RESOURCE, java.util.List.of(501L));
        verify(andonCache).evictDetailsAfterCommit(
                AndonRedisKeyConstants.CONFIGURATION_RESOURCE, java.util.List.of(502L));
        verify(andonCache).evictDetailsAfterCommit(
                AndonRedisKeyConstants.EVENT_RESOURCE, java.util.List.of(503L));
    }

    @Test
    @DisplayName("删除安灯类型：存在配置引用时拒绝删除")
    void deleteTypeRejectsConfigurationReference() {
        when(typeRepository.findByIdAndDeletedFalseForUpdate(TYPE_ID))
                .thenReturn(Optional.of(buildTypeEntity()));
        // 只设置配置引用即可触发保护，其他引用计数无需参与本用例。
        when(typeRepository.countConfigurationsByTypeId(TYPE_ID)).thenReturn(1L);

        // 被引用类型不得进入逻辑删除或编码改写，确保既有配置仍可解析其类型。
        assertThatThrownBy(() -> typeService.deleteType(TYPE_ID))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                AndonErrorCodeConstants.TYPE_HAS_REFERENCES));
        verify(typeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("删除安灯类型：无引用时使用保留前缀重命名并逻辑删除")
    void deleteTypeRenamesCodeAndMarksDeletedWhenNoReferences() {
        AndonTypeEntity type = buildTypeEntity();
        when(typeRepository.findByIdAndDeletedFalseForUpdate(TYPE_ID))
                .thenReturn(Optional.of(type));
        // 三类业务引用全部归零，证明类型已具备安全删除条件。
        when(typeRepository.countConfigurationsByTypeId(TYPE_ID)).thenReturn(0L);
        when(typeRepository.countReasonsByTypeId(TYPE_ID)).thenReturn(0L);
        when(typeRepository.countEventsByTypeId(TYPE_ID)).thenReturn(0L);
        // 预期保留编码不存在，覆盖一次生成即可成功释放原业务编码的路径。
        when(typeRepository.existsByTypeCode("__DELETED_B4")).thenReturn(false);

        typeService.deleteType(TYPE_ID);

        // 同时验证编码释放、禁用和逻辑删除，避免仅设置删除标记造成唯一键占用。
        assertThat(type.getTypeCode()).isEqualTo("__DELETED_B4");
        assertThat(type.getEnabledStatus()).isZero();
        assertThat(type.getDeleted()).isTrue();
        verify(typeRepository).saveAndFlush(type);
    }

    @Test
    @DisplayName("分页查询安灯类型：无数据时返回空分页且不查列表")
    void getTypePageReturnsEmptyPageWhenNoData() {
        // 构造正常分页参数，并用总数为零触发查询短路分支。
        AndonTypePageReqVO request = new AndonTypePageReqVO();
        request.setPageNo(1);
        request.setPageSize(20);
        when(typeRepository.count(any(Specification.class))).thenReturn(0L);

        PageResult<?> pageResult = typeService.getTypePage(request);

        // 空结果应直接组装分页对象，不再执行无意义的列表 SQL。
        assertThat(pageResult.getTotal()).isZero();
        assertThat(pageResult.getList()).isEmpty();
        verify(typeRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    /**
     * 构造满足协助处理模式全部规则的保存请求，供用例按需移除或覆盖单一字段。
     */
    private AndonTypeSaveReqVO buildAssistanceTypeRequest() {
        AndonTypeSaveReqVO request = new AndonTypeSaveReqVO();
        request.setTypeCode("ASSISTANCE");
        request.setTypeName("协助处理异常");
        request.setExceptionCategory("EQUIPMENT");
        request.setHandlingMode("ASSISTANCE");
        request.setResponseMinutes(30);
        request.setResponsibleRoleCode("ADMIN");
        request.setNotificationChannels("IN_APP");
        request.setEnabledStatus(1);
        return request;
    }

    /**
     * 构造已启用、未删除的现有类型，作为更新和删除状态变化的统一起点。
     */
    private AndonTypeEntity buildTypeEntity() {
        AndonTypeEntity type = new AndonTypeEntity();
        type.setId(TYPE_ID);
        type.setTypeCode("ASSISTANCE");
        type.setTypeName("协助处理异常");
        type.setExceptionCategory("EQUIPMENT");
        type.setHandlingMode("ASSISTANCE");
        type.setEnabledStatus(1);
        type.setDeleted(false);
        return type;
    }
}
