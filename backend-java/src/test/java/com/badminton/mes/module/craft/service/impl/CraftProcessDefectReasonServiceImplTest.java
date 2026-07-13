package com.badminton.mes.module.craft.service.impl;

import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.constants.CraftErrorCodeConstants;
import com.badminton.mes.module.craft.controller.vo.CraftProcessDefectReasonSaveReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessDefectReasonUpdateReqVO;
import com.badminton.mes.module.craft.dal.entity.CraftProcessDefectReasonEntity;
import com.badminton.mes.module.craft.dal.entity.CraftProcessEntity;
import com.badminton.mes.module.craft.dal.repository.CraftProcessDefectReasonRepository;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.craft.dal.redis.CraftCache;
import com.badminton.mes.module.craft.service.CraftProcessAuditService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CraftProcessDefectReasonServiceImpl} 单元测试。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@ExtendWith(MockitoExtension.class)
class CraftProcessDefectReasonServiceImplTest {

    private static final Long PROCESS_ID = 100L;

    private static final Long REASON_ID = 300L;

    @Mock
    private CraftProcessRepository processRepository;

    @Mock
    private CraftProcessDefectReasonRepository reasonRepository;

    @Mock
    private CraftProcessAuditService auditService;

    @Mock
    private CraftCache craftCache;

    private CraftProcessDefectReasonServiceImpl reasonService;

    @BeforeEach
    void setUp() {
        reasonService = new CraftProcessDefectReasonServiceImpl(
                processRepository, reasonRepository, auditService, craftCache);
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
    @DisplayName("修改不良原因：客户端版本落后时拒绝覆盖")
    void updateReasonRejectsStaleVersion() {
        when(reasonRepository.findByIdAndProcessIdAndDeletedFalse(REASON_ID, PROCESS_ID))
                .thenReturn(Optional.of(buildReason(2)));
        CraftProcessDefectReasonUpdateReqVO reqVO = buildUpdateReqVO();
        reqVO.setVersion(1);

        assertThatThrownBy(() -> reasonService.updateProcessDefectReason(
                PROCESS_ID, REASON_ID, reqVO))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                CraftErrorCodeConstants.PROCESS_DEFECT_REASON_CONCURRENT_MODIFICATION));
        verify(reasonRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("删除不良原因：仅设置逻辑删除标志并保留原编码")
    void deleteReasonMarksDeletedWithoutRenaming() {
        CraftProcessDefectReasonEntity reason = buildReason(0);
        when(reasonRepository.findByIdAndProcessIdAndDeletedFalse(REASON_ID, PROCESS_ID))
                .thenReturn(Optional.of(reason));

        reasonService.deleteProcessDefectReason(PROCESS_ID, REASON_ID, 0);

        assertThat(reason.getDeleted()).isTrue();
        assertThat(reason.getReasonCode()).isEqualTo("BROKEN-FEATHER");
        verify(craftCache).evictProcessDefectReasonsAfterCommit(PROCESS_ID);
    }

    private CraftProcessDefectReasonUpdateReqVO buildUpdateReqVO() {
        CraftProcessDefectReasonUpdateReqVO reqVO = new CraftProcessDefectReasonUpdateReqVO();
        fillFields(reqVO);
        return reqVO;
    }

    private void fillFields(CraftProcessDefectReasonSaveReqVO reqVO) {
        reqVO.setReasonCode("BROKEN-FEATHER");
        reqVO.setReasonName("断羽");
        reqVO.setStatus(1);
    }

    private CraftProcessDefectReasonEntity buildReason(Integer version) {
        CraftProcessDefectReasonEntity reason = new CraftProcessDefectReasonEntity();
        reason.setId(REASON_ID);
        reason.setProcessId(PROCESS_ID);
        reason.setReasonCode("BROKEN-FEATHER");
        reason.setReasonName("断羽");
        reason.setStatus(1);
        reason.setVersion(version);
        return reason;
    }
}
