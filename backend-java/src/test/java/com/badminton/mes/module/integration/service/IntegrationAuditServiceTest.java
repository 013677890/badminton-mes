package com.badminton.mes.module.integration.service;

import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.integration.dal.entity.IntegrationWriteLogEntity;
import com.badminton.mes.module.integration.dal.repository.IntegrationWriteLogRepository;
import com.badminton.mes.module.integration.enums.IntegrationInterfaceTypeEnum;
import com.badminton.mes.module.integration.enums.IntegrationWriteStatusEnum;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

/**
 * {@link IntegrationAuditService} 单元测试。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@ExtendWith(MockitoExtension.class)
class IntegrationAuditServiceTest {

    @Mock
    private IntegrationWriteLogRepository writeLogRepository;

    private IntegrationAuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new IntegrationAuditService(
                writeLogRepository, JsonMapper.builder().build());
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(9L);
        SecurityContextHolder.set("integration-audit-test", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("重复日志：并发重复不记录失败错误码和错误信息")
    void recordDuplicateDoesNotWriteFailureFields() {
        doAnswer(invocation -> {
            IntegrationWriteLogEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return entity;
        }).when(writeLogRepository).saveAndFlush(any(IntegrationWriteLogEntity.class));

        Long logId = auditService.recordDuplicate(
                IntegrationInterfaceTypeEnum.WORK_ORDER_WRITE,
                "ERP-MAIN", "ERP-WO-001", "{}", 60L, "WO202607110002");

        assertThat(logId).isEqualTo(100L);
        ArgumentCaptor<IntegrationWriteLogEntity> captor =
                ArgumentCaptor.forClass(IntegrationWriteLogEntity.class);
        verify(writeLogRepository).saveAndFlush(captor.capture());
        IntegrationWriteLogEntity log = captor.getValue();
        assertThat(log.getWriteStatus()).isEqualTo(IntegrationWriteStatusEnum.DUPLICATE.getStatus());
        assertThat(log.getErrorCode()).isNull();
        assertThat(log.getErrorMessage()).isNull();
    }
}
