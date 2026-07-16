package com.badminton.mes.stress;

import com.badminton.mes.module.andon.dal.redis.AndonRedisKeyConstants;
import com.badminton.mes.module.device.dal.redis.DeviceRedisKeyConstants;
import com.badminton.mes.module.equipment.dal.redis.EquipmentRedisKeyConstants;
import com.badminton.mes.module.quality.dal.redis.QualityRedisKeyConstants;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Set;

/** C 组设备、接入、质量和安灯缓存 Key 并发压力测试。 @author 范家权 */
@Tag("stress")
@Timeout(90)
class CTeamConcurrencyStressTest {

    private static final int OPERATIONS = Integer.getInteger("mes.stress.operations", 10_000);

    @Test
    void equipmentAndDeviceKeysStayInsideTheirNamespaces() throws Exception {
        ConcurrentStressRunner.run("c-equipment-device-keys", OPERATIONS, index -> {
            long id = index + 1L;
            String equipment = EquipmentRedisKeyConstants.detailKey(
                    EquipmentRedisKeyConstants.LEDGER_RESOURCE, id);
            String device = DeviceRedisKeyConstants.detailKey(
                    DeviceRedisKeyConstants.ACCESS_CONFIG_RESOURCE, id);
            if (!equipment.equals("mes:equipment:ledger:" + id)
                    || !device.equals("mes:device:access_config:" + id)) {
                throw new AssertionError("cache namespace collision");
            }
        });
    }

    @Test
    void qualityAndAndonVersionKeysRemainOneToOneWithDetailKeys() throws Exception {
        ConcurrentStressRunner.run("c-quality-andon-keys", OPERATIONS, index -> {
            long id = index + 1L;
            String quality = QualityRedisKeyConstants.detailVersionKey(
                    QualityRedisKeyConstants.INSPECTION_RECORD_RESOURCE, id);
            String andon = AndonRedisKeyConstants.detailVersionKey(
                    AndonRedisKeyConstants.EVENT_RESOURCE, id);
            if (!quality.equals("mes:quality:inspection_record:" + id + ":version")
                    || !andon.equals("mes:andon:event:" + id + ":version")) {
                throw new AssertionError("cache version key mismatch");
            }
        });
    }

    @Test
    void allCModuleKeysRemainDistinctForSameBusinessId() throws Exception {
        ConcurrentStressRunner.run("c-key-isolation", OPERATIONS, index -> {
            long id = index + 1L;
            Set<String> keys = Set.of(
                    EquipmentRedisKeyConstants.detailKey(
                            EquipmentRedisKeyConstants.CATEGORY_RESOURCE, id),
                    DeviceRedisKeyConstants.detailKey(
                            DeviceRedisKeyConstants.COUNT_RECORD_RESOURCE, id),
                    QualityRedisKeyConstants.detailKey(
                            QualityRedisKeyConstants.INSPECTION_ITEM_RESOURCE, id),
                    AndonRedisKeyConstants.detailKey(AndonRedisKeyConstants.TYPE_RESOURCE, id));
            if (keys.size() != 4) {
                throw new AssertionError("C module key collision");
            }
        });
    }
}
