package com.badminton.mes.module.craft.service.impl;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.constants.CraftErrorCodeConstants;
import com.badminton.mes.module.craft.controller.vo.CraftProcessChangeLogPageReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessChangeLogRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessSaveReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessStatusReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessUpdateReqVO;
import com.badminton.mes.module.craft.dal.entity.CraftProcessChangeLogEntity;
import com.badminton.mes.module.craft.dal.entity.CraftProcessEntity;
import com.badminton.mes.module.craft.dal.repository.CraftProcessChangeLogRepository;
import com.badminton.mes.module.craft.dal.repository.CraftProcessDefectReasonRepository;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.craft.dal.repository.CraftProcessSopRepository;
import com.badminton.mes.module.craft.dal.repository.CraftQualityPlanReferenceRepository;
import com.badminton.mes.module.craft.dal.repository.CraftRouteDetailRepository;
import com.badminton.mes.module.craft.dal.redis.CraftCache;
import com.badminton.mes.module.craft.enums.CraftProcessChangeTypeEnum;
import com.badminton.mes.module.craft.service.CraftProcessAuditService;
import com.badminton.mes.module.equipment.dal.repository.EquipmentCategoryRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CraftProcessServiceImpl} 单元测试。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@ExtendWith(MockitoExtension.class)
class CraftProcessServiceImplTest {

    private static final Long PROCESS_ID = 100L;

    private static final Long OPERATOR_ID = 9L;

    @Mock
    private CraftProcessRepository processRepository;

    @Mock
    private CraftProcessChangeLogRepository changeLogRepository;

    @Mock
    private CraftProcessSopRepository sopRepository;

    @Mock
    private CraftProcessDefectReasonRepository defectReasonRepository;

    @Mock
    private CraftRouteDetailRepository routeDetailRepository;

    @Mock
    private CraftQualityPlanReferenceRepository qualityPlanRepository;

    @Mock
    private EquipmentCategoryRepository equipmentCategoryRepository;

    @Mock
    private CraftProcessAuditService auditService;

    @Mock
    private CraftCache craftCache;

    private CraftProcessServiceImpl processService;

    @BeforeEach
    void setUp() {
        processService = new CraftProcessServiceImpl(processRepository, changeLogRepository,
                sopRepository, defectReasonRepository, routeDetailRepository, qualityPlanRepository,
                equipmentCategoryRepository, auditService, craftCache);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(OPERATOR_ID);
        SecurityContextHolder.set("unit-test-token", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("创建工序：规范化编码并写入创建审计")
    void createProcessNormalizesAndWritesAudit() {
        CraftProcessSaveReqVO reqVO = buildSaveReqVO();
        reqVO.setProcessCode("  feather-fix ");
        doAnswer(invocation -> {
            CraftProcessEntity entity = invocation.getArgument(0);
            entity.setId(PROCESS_ID);
            entity.setVersion(0);
            return entity;
        }).when(processRepository).saveAndFlush(any(CraftProcessEntity.class));

        Long id = processService.createProcess(reqVO);

        assertThat(id).isEqualTo(PROCESS_ID);
        ArgumentCaptor<CraftProcessEntity> captor = ArgumentCaptor.forClass(CraftProcessEntity.class);
        verify(processRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getProcessCode()).isEqualTo("FEATHER-FIX");
        verify(auditService).record(eq(PROCESS_ID), eq(CraftProcessChangeTypeEnum.CREATE),
                eq(null), any(), eq("创建工序档案"), eq(OPERATOR_ID));
        verify(craftCache).evictProcessAfterCommit(PROCESS_ID);
    }

    @Test
    @DisplayName("修改工序：客户端版本落后时拒绝覆盖新数据")
    void updateProcessRejectsStaleClientVersion() {
        CraftProcessEntity process = buildProcess(1);
        when(processRepository.findByIdAndDeletedFalseForUpdate(PROCESS_ID)).thenReturn(Optional.of(process));
        CraftProcessUpdateReqVO reqVO = buildUpdateReqVO();
        reqVO.setVersion(0);

        assertThatThrownBy(() -> processService.updateProcess(PROCESS_ID, reqVO))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isSameAs(CraftErrorCodeConstants.PROCESS_CONCURRENT_MODIFICATION));
        verify(processRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("状态幂等请求：版本落后时仍返回并发冲突")
    void updateStatusChecksVersionBeforeIdempotence() {
        when(processRepository.findByIdAndDeletedFalseForUpdate(PROCESS_ID))
                .thenReturn(Optional.of(buildProcess(1)));
        CraftProcessStatusReqVO reqVO = new CraftProcessStatusReqVO();
        reqVO.setVersion(0);
        reqVO.setStatus(1);
        reqVO.setReason("保持启用");

        assertThatThrownBy(() -> processService.updateProcessStatus(PROCESS_ID, reqVO))
                .isInstanceOf(ServiceException.class);
        verify(processRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("停用工序：仍被生效路线引用时拒绝状态变更")
    void disableProcessRejectsEffectiveRouteReference() {
        when(processRepository.findByIdAndDeletedFalseForUpdate(PROCESS_ID))
                .thenReturn(Optional.of(buildProcess(0)));
        when(routeDetailRepository.existsEffectiveRouteByProcessId(PROCESS_ID, 1)).thenReturn(true);
        CraftProcessStatusReqVO reqVO = new CraftProcessStatusReqVO();
        reqVO.setVersion(0);
        reqVO.setStatus(0);
        reqVO.setReason("工序淘汰");

        assertThatThrownBy(() -> processService.updateProcessStatus(PROCESS_ID, reqVO))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                CraftErrorCodeConstants.PROCESS_RULE_REFERENCED_BY_EFFECTIVE_ROUTE));
        verify(processRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("创建工序：检验方案不存在或未启用时拒绝保存")
    void createProcessRejectsUnavailableQualityPlan() {
        CraftProcessSaveReqVO reqVO = buildSaveReqVO();
        reqVO.setQualityRequired(true);
        reqVO.setQualityPlanId(20L);
        when(qualityPlanRepository.existsByIdAndStatusAndDeletedFalse(20L, 1)).thenReturn(false);

        assertThatThrownBy(() -> processService.createProcess(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                CraftErrorCodeConstants.PROCESS_QUALITY_PLAN_NOT_AVAILABLE));
    }

    @Test
    @DisplayName("删除工序：存在未删除 SOP 时拒绝删除")
    void deleteProcessRejectsActiveBindings() {
        when(processRepository.findByIdAndDeletedFalseForUpdate(PROCESS_ID))
                .thenReturn(Optional.of(buildProcess(0)));
        when(sopRepository.existsByProcessIdAndDeletedFalse(PROCESS_ID)).thenReturn(true);

        assertThatThrownBy(() -> processService.deleteProcess(PROCESS_ID, 0))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isSameAs(CraftErrorCodeConstants.PROCESS_HAS_ACTIVE_BINDINGS));
        verify(processRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("删除工序：仍被工艺路线引用时拒绝删除")
    void deleteProcessRejectsRouteReference() {
        when(processRepository.findByIdAndDeletedFalseForUpdate(PROCESS_ID))
                .thenReturn(Optional.of(buildProcess(0)));
        when(routeDetailRepository.existsByProcessIdAndDeletedFalse(PROCESS_ID)).thenReturn(true);

        assertThatThrownBy(() -> processService.deleteProcess(PROCESS_ID, 0))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isSameAs(CraftErrorCodeConstants.PROCESS_REFERENCED_BY_ROUTE));
        verify(processRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("删除工序：逻辑删除但不再重命名编码")
    void deleteProcessMarksDeletedWithoutRenamingCode() {
        CraftProcessEntity process = buildProcess(0);
        when(processRepository.findByIdAndDeletedFalseForUpdate(PROCESS_ID)).thenReturn(Optional.of(process));

        processService.deleteProcess(PROCESS_ID, 0);

        assertThat(process.getDeleted()).isTrue();
        assertThat(process.getProcessCode()).isEqualTo("FEATHER-FIX");
        verify(auditService).record(eq(PROCESS_ID), eq(CraftProcessChangeTypeEnum.DELETE),
                any(), eq(null), eq("删除工序档案"), eq(OPERATOR_ID));
        verify(craftCache).evictProcessAggregateAfterCommit(PROCESS_ID);
    }

    @Test
    @DisplayName("工序详情：缓存命中时不访问数据库")
    void getProcessReturnsCacheHitWithoutDatabaseQuery() {
        CraftProcessRespVO cached = new CraftProcessRespVO();
        cached.setId(PROCESS_ID);
        when(craftCache.getProcess(PROCESS_ID)).thenReturn(Optional.of(cached));

        CraftProcessRespVO result = processService.getProcess(PROCESS_ID);

        assertThat(result).isSameAs(cached);
        verify(processRepository, never()).findByIdAndDeletedFalse(PROCESS_ID);
    }

    @Test
    @DisplayName("工序详情：缓存未命中时查询数据库并回填")
    void getProcessLoadsDatabaseAndPopulatesCacheOnMiss() {
        when(processRepository.findByIdAndDeletedFalse(PROCESS_ID))
                .thenReturn(Optional.of(buildProcess(0)));

        CraftProcessRespVO result = processService.getProcess(PROCESS_ID);

        assertThat(result.getId()).isEqualTo(PROCESS_ID);
        verify(craftCache).putProcess(result);
    }

    @Test
    @DisplayName("保存工序：无法识别的完整性异常不伪装成编码重复")
    void saveProcessKeepsUnknownIntegrityViolation() {
        when(processRepository.saveAndFlush(any(CraftProcessEntity.class)))
                .thenThrow(new DataIntegrityViolationException("unknown constraint"));

        assertThatThrownBy(() -> processService.createProcess(buildSaveReqVO()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("变更日志：分页返回并按 id 倒序查询")
    void getChangeLogPageReturnsPagedResult() {
        CraftProcessChangeLogPageReqVO reqVO = new CraftProcessChangeLogPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        CraftProcessChangeLogEntity log = new CraftProcessChangeLogEntity();
        log.setId(5L);
        when(processRepository.existsById(PROCESS_ID)).thenReturn(true);
        when(changeLogRepository.countByProcessIdAndDeletedFalse(PROCESS_ID)).thenReturn(1L);
        when(changeLogRepository.findByProcessIdAndDeletedFalse(eq(PROCESS_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        PageResult<CraftProcessChangeLogRespVO> result =
                processService.getProcessChangeLogPage(PROCESS_ID, reqVO);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).singleElement()
                .extracting(CraftProcessChangeLogRespVO::getId).isEqualTo(5L);
    }

    private CraftProcessSaveReqVO buildSaveReqVO() {
        CraftProcessSaveReqVO reqVO = new CraftProcessSaveReqVO();
        fillSaveFields(reqVO);
        return reqVO;
    }

    private CraftProcessUpdateReqVO buildUpdateReqVO() {
        CraftProcessUpdateReqVO reqVO = new CraftProcessUpdateReqVO();
        fillSaveFields(reqVO);
        return reqVO;
    }

    private void fillSaveFields(CraftProcessSaveReqVO reqVO) {
        reqVO.setProcessCode("FEATHER-FIX");
        reqVO.setProcessName("植毛");
        reqVO.setProcessType("PROCESSING");
        reqVO.setStandardTimeSeconds(120);
        reqVO.setKeyProcess(true);
        reqVO.setQualityRequired(false);
        reqVO.setScanRequired(true);
        reqVO.setPieceRateEnabled(true);
    }

    private CraftProcessEntity buildProcess(Integer version) {
        CraftProcessEntity process = new CraftProcessEntity();
        process.setId(PROCESS_ID);
        process.setProcessCode("FEATHER-FIX");
        process.setProcessName("植毛");
        process.setProcessType("PROCESSING");
        process.setStandardTimeSeconds(120);
        process.setKeyProcess(true);
        process.setQualityRequired(false);
        process.setScanRequired(true);
        process.setPieceRateEnabled(true);
        process.setStatus(1);
        process.setVersion(version);
        return process;
    }
}
