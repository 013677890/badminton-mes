package com.badminton.mes.module.craft.service.impl;

import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.constants.CraftErrorCodeConstants;
import com.badminton.mes.module.craft.controller.vo.CraftProcessSopSaveReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessSopUpdateReqVO;
import com.badminton.mes.module.craft.dal.entity.CraftProcessEntity;
import com.badminton.mes.module.craft.dal.entity.CraftProcessSopEntity;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.craft.dal.repository.CraftProcessSopRepository;
import com.badminton.mes.module.craft.service.CraftProcessAuditService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CraftProcessSopServiceImpl} 单元测试。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@ExtendWith(MockitoExtension.class)
class CraftProcessSopServiceImplTest {

    private static final Long PROCESS_ID = 100L;

    private static final Long SOP_ID = 200L;

    @Mock
    private CraftProcessRepository processRepository;

    @Mock
    private CraftProcessSopRepository sopRepository;

    @Mock
    private CraftProcessAuditService auditService;

    private CraftProcessSopServiceImpl sopService;

    @BeforeEach
    void setUp() {
        sopService = new CraftProcessSopServiceImpl(processRepository, sopRepository, auditService);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(9L);
        SecurityContextHolder.set("unit-test-token", loginUser);
        when(processRepository.findByIdAndDeletedFalse(PROCESS_ID))
                .thenReturn(Optional.of(new CraftProcessEntity()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("新增 SOP：规范化编码并返回关联主键")
    void createSopNormalizesCode() {
        CraftProcessSopSaveReqVO reqVO = buildSaveReqVO();
        reqVO.setSopCode(" sop-01 ");
        doAnswer(invocation -> {
            CraftProcessSopEntity entity = invocation.getArgument(0);
            entity.setId(SOP_ID);
            entity.setVersion(0);
            return entity;
        }).when(sopRepository).saveAndFlush(any(CraftProcessSopEntity.class));

        Long id = sopService.createProcessSop(PROCESS_ID, reqVO);

        assertThat(id).isEqualTo(SOP_ID);
        assertThat(reqVO.getSopCode()).isEqualTo("SOP-01");
    }

    @Test
    @DisplayName("修改 SOP：客户端版本落后时拒绝覆盖")
    void updateSopRejectsStaleVersion() {
        when(sopRepository.findByIdAndProcessIdAndDeletedFalse(SOP_ID, PROCESS_ID))
                .thenReturn(Optional.of(buildSop(2)));
        CraftProcessSopUpdateReqVO reqVO = buildUpdateReqVO();
        reqVO.setVersion(1);

        assertThatThrownBy(() -> sopService.updateProcessSop(PROCESS_ID, SOP_ID, reqVO))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                CraftErrorCodeConstants.PROCESS_SOP_CONCURRENT_MODIFICATION));
        verify(sopRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("删除 SOP：仅设置逻辑删除标志并保留原编码")
    void deleteSopMarksDeletedWithoutRenaming() {
        CraftProcessSopEntity sop = buildSop(0);
        when(sopRepository.findByIdAndProcessIdAndDeletedFalse(SOP_ID, PROCESS_ID))
                .thenReturn(Optional.of(sop));

        sopService.deleteProcessSop(PROCESS_ID, SOP_ID, 0);

        assertThat(sop.getDeleted()).isTrue();
        assertThat(sop.getSopCode()).isEqualTo("SOP-01");
    }

    @Test
    @DisplayName("保存 SOP：事务重叠触发 JPA 乐观锁时转换为业务错误")
    void updateSopTranslatesJpaOptimisticLockFailure() {
        when(sopRepository.findByIdAndProcessIdAndDeletedFalse(SOP_ID, PROCESS_ID))
                .thenReturn(Optional.of(buildSop(0)));
        when(sopRepository.saveAndFlush(any(CraftProcessSopEntity.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(CraftProcessSopEntity.class, SOP_ID));
        CraftProcessSopUpdateReqVO reqVO = buildUpdateReqVO();
        reqVO.setVersion(0);

        assertThatThrownBy(() -> sopService.updateProcessSop(PROCESS_ID, SOP_ID, reqVO))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                CraftErrorCodeConstants.PROCESS_SOP_CONCURRENT_MODIFICATION));
    }

    private CraftProcessSopSaveReqVO buildSaveReqVO() {
        CraftProcessSopSaveReqVO reqVO = new CraftProcessSopSaveReqVO();
        fillFields(reqVO);
        return reqVO;
    }

    private CraftProcessSopUpdateReqVO buildUpdateReqVO() {
        CraftProcessSopUpdateReqVO reqVO = new CraftProcessSopUpdateReqVO();
        fillFields(reqVO);
        return reqVO;
    }

    private void fillFields(CraftProcessSopSaveReqVO reqVO) {
        reqVO.setSopCode("SOP-01");
        reqVO.setSopName("植毛作业指导书");
        reqVO.setSopVersion("V1.0");
        reqVO.setFileUrl("/files/sop-01.pdf");
        reqVO.setStatus(1);
    }

    private CraftProcessSopEntity buildSop(Integer version) {
        CraftProcessSopEntity sop = new CraftProcessSopEntity();
        sop.setId(SOP_ID);
        sop.setProcessId(PROCESS_ID);
        sop.setSopCode("SOP-01");
        sop.setSopName("植毛作业指导书");
        sop.setSopVersion("V1.0");
        sop.setFileUrl("/files/sop-01.pdf");
        sop.setStatus(1);
        sop.setVersion(version);
        return sop;
    }
}
