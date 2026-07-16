package com.badminton.mes.group.c;

import com.badminton.mes.module.andon.dal.redis.AndonRedisKeyConstants;
import com.badminton.mes.module.device.dal.redis.DeviceRedisKeyConstants;
import com.badminton.mes.module.equipment.dal.redis.EquipmentRedisKeyConstants;
import com.badminton.mes.module.quality.dal.redis.QualityRedisKeyConstants;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** C 组设备、接入、质量和安灯详情缓存 Key/版本 Key 契约测试。 @author 范家权 */
class CTeamCacheKeyContractTest {

    @Test
    void detailAndVersionKeysArePairedForEveryCModule() {
        assertThat(EquipmentRedisKeyConstants.detailVersionKey("ledger", 1L))
                .isEqualTo(EquipmentRedisKeyConstants.detailKey("ledger", 1L) + ":version");
        assertThat(DeviceRedisKeyConstants.detailVersionKey("count_record", 1L))
                .isEqualTo(DeviceRedisKeyConstants.detailKey("count_record", 1L) + ":version");
        assertThat(QualityRedisKeyConstants.detailVersionKey("inspection_plan", 1L))
                .isEqualTo(QualityRedisKeyConstants.detailKey("inspection_plan", 1L) + ":version");
        assertThat(AndonRedisKeyConstants.detailVersionKey("event", 1L))
                .isEqualTo(AndonRedisKeyConstants.detailKey("event", 1L) + ":version");
    }

    @Test
    void sameBusinessIdStaysIsolatedByModuleNamespace() {
        Set<String> keys = Set.of(
                EquipmentRedisKeyConstants.detailKey("ledger", 99L),
                DeviceRedisKeyConstants.detailKey("count_record", 99L),
                QualityRedisKeyConstants.detailKey("inspection_plan", 99L),
                AndonRedisKeyConstants.detailKey("event", 99L));
        assertThat(keys).hasSize(4);
    }

    @Test
    void allCDetailCachesUseExplicitThirtyMinuteTtl() {
        assertThat(EquipmentRedisKeyConstants.EQUIPMENT_DETAIL_TTL).isEqualTo(Duration.ofMinutes(30));
        assertThat(DeviceRedisKeyConstants.DEVICE_DETAIL_TTL).isEqualTo(Duration.ofMinutes(30));
        assertThat(QualityRedisKeyConstants.QUALITY_DETAIL_TTL).isEqualTo(Duration.ofMinutes(30));
        assertThat(AndonRedisKeyConstants.ANDON_DETAIL_TTL).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void resourceNamesRemainStableForCrossResourceInvalidation() {
        assertThat(EquipmentRedisKeyConstants.MAINTENANCE_RECORD_RESOURCE)
                .isEqualTo("maintenance_record");
        assertThat(DeviceRedisKeyConstants.COUNT_EXCEPTION_RESOURCE).isEqualTo("count_exception");
        assertThat(QualityRedisKeyConstants.INSPECTION_RECORD_RESOURCE)
                .isEqualTo("inspection_record");
        assertThat(AndonRedisKeyConstants.CONFIGURATION_RESOURCE).isEqualTo("configuration");
    }
}
