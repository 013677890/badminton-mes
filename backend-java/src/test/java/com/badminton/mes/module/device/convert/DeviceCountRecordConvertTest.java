package com.badminton.mes.module.device.convert;

import java.time.LocalDateTime;

import com.badminton.mes.module.device.controller.vo.DeviceCountReportReqVO;
import com.badminton.mes.module.device.dal.entity.DeviceAccessConfigEntity;
import com.badminton.mes.module.device.dal.entity.DeviceCountRecordEntity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 设备计数上报快照转换单元测试。 */
class DeviceCountRecordConvertTest {

    @Test
    void toEntityCombinesTrustedConfigWithReportedPayload() {
        DeviceAccessConfigEntity config = new DeviceAccessConfigEntity();
        config.setId(11L);
        config.setEquipmentId(22L);
        config.setCollectionPointCode("COUNT-A");
        config.setProcessId(33L);

        DeviceCountReportReqVO request = new DeviceCountReportReqVO();
        request.setEquipmentCode("EQ-001");
        request.setCollectedAt(LocalDateTime.of(2026, 7, 14, 8, 30));
        request.setSerialNumber("SN-100");
        request.setCountValue(120L);
        request.setRuntimeStatus("RUNNING");
        request.setRawPayload("{\"count\":120}");

        DeviceCountRecordEntity entity =
                DeviceCountRecordConvert.toEntity(request, config, "EQ-001:SN-100");

        assertThat(entity.getAccessConfigId()).isEqualTo(11L);
        assertThat(entity.getEquipmentId()).isEqualTo(22L);
        assertThat(entity.getEquipmentCodeSnapshot()).isEqualTo("EQ-001");
        assertThat(entity.getCollectionPointCodeSnapshot()).isEqualTo("COUNT-A");
        assertThat(entity.getProcessId()).isEqualTo(33L);
        assertThat(entity.getRawCount()).isEqualTo(120L);
        assertThat(entity.getDeduplicationKey()).isEqualTo("EQ-001:SN-100");
    }

    @Test
    void reportResponseCarriesProcessingOutcome() {
        DeviceCountRecordEntity entity = new DeviceCountRecordEntity();
        entity.setId(41L);
        entity.setIncrementCount(3L);
        entity.setMatchStatus("MATCHED");
        entity.setReportStatus("REPORTED");

        assertThat(DeviceCountRecordConvert.toReportRespVO(entity, null, "accepted"))
                .extracting("countRecordId", "incrementCount", "matchStatus", "reportStatus", "processingMessage")
                .containsExactly(41L, 3L, "MATCHED", "REPORTED", "accepted");
    }
}
