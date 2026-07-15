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
 * <p>覆盖安灯类型规则校验、引用保护、删除编码保护和空分页等核心业务规则。
 */
@ExtendWith(MockitoExtension.class)
class AndonTypeServiceImplTest {

    private static final Long TYPE_ID = 400L;

    @Mock
    private AndonTypeRepository typeRepository;

    @Mock
    private AndonReasonRepository reasonRepository;

    @Mock
    private AndonConfigurationRepository configurationRepository;

    @Mock
    private AndonEventRepository eventRepository;

    @Mock
    private AndonCache andonCache;

    private AndonTypeServiceImpl typeService;

    @BeforeEach
    void setUp() {
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
        AndonTypeSaveReqVO request = buildAssistanceTypeRequest();
        request.setResponsibleRoleCode(null);

        assertThatThrownBy(() -> typeService.createType(request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                AndonErrorCodeConstants.TYPE_RULE_INVALID));
        verify(typeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("创建安灯类型：默认灯控关闭并返回主键")
    void createTypeDefaultsLightControlAndReturnsId() {
        AndonTypeSaveReqVO request = buildAssistanceTypeRequest();
        request.setLightControlEnabled(null);
        when(typeRepository.existsByTypeCodeAndDeletedFalse(request.getTypeCode()))
                .thenReturn(false);
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
        when(typeRepository.countConfigurationsByTypeId(TYPE_ID)).thenReturn(1L);

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
        when(typeRepository.countConfigurationsByTypeId(TYPE_ID)).thenReturn(0L);
        when(typeRepository.countReasonsByTypeId(TYPE_ID)).thenReturn(0L);
        when(typeRepository.countEventsByTypeId(TYPE_ID)).thenReturn(0L);
        when(typeRepository.existsByTypeCode("__DELETED_B4")).thenReturn(false);

        typeService.deleteType(TYPE_ID);

        assertThat(type.getTypeCode()).isEqualTo("__DELETED_B4");
        assertThat(type.getEnabledStatus()).isZero();
        assertThat(type.getDeleted()).isTrue();
        verify(typeRepository).saveAndFlush(type);
    }

    @Test
    @DisplayName("分页查询安灯类型：无数据时返回空分页且不查列表")
    void getTypePageReturnsEmptyPageWhenNoData() {
        AndonTypePageReqVO request = new AndonTypePageReqVO();
        request.setPageNo(1);
        request.setPageSize(20);
        when(typeRepository.count(any(Specification.class))).thenReturn(0L);

        PageResult<?> pageResult = typeService.getTypePage(request);

        assertThat(pageResult.getTotal()).isZero();
        assertThat(pageResult.getList()).isEmpty();
        verify(typeRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

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
