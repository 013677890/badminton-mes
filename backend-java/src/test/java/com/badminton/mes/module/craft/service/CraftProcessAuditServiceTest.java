package com.badminton.mes.module.craft.service;

import com.badminton.mes.module.craft.dal.entity.CraftProcessChangeLogEntity;
import com.badminton.mes.module.craft.dal.repository.CraftProcessChangeLogRepository;
import com.badminton.mes.module.craft.enums.CraftProcessChangeTypeEnum;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * {@link CraftProcessAuditService} 单元测试。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@ExtendWith(MockitoExtension.class)
class CraftProcessAuditServiceTest {

    @Mock
    private CraftProcessChangeLogRepository changeLogRepository;

    @Test
    @DisplayName("审计快照：合法长 URL 转义后超过 1024 字符仍完整交给 TEXT 列")
    void recordKeepsLongEscapedSnapshot() {
        CraftProcessAuditService auditService =
                new CraftProcessAuditService(changeLogRepository, new ObjectMapper());
        String longEscapedUrl = "\\\"".repeat(256);

        auditService.record(100L, CraftProcessChangeTypeEnum.SOP_BINDING,
                null, new LongUrlSnapshot(longEscapedUrl), "修改 SOP", 9L);

        ArgumentCaptor<CraftProcessChangeLogEntity> captor =
                ArgumentCaptor.forClass(CraftProcessChangeLogEntity.class);
        verify(changeLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAfterSnapshot()).hasSizeGreaterThan(1024);
        assertThat(captor.getValue().getAfterSnapshot()).contains("fileUrl");
    }

    /**
     * 测试用长 URL 快照。
     *
     * @param fileUrl 文件地址
     * @author 张竹灏
     * @date 2026/07/10
     */
    private record LongUrlSnapshot(String fileUrl) {
    }
}
