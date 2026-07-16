package com.badminton.mes.group.a;

import com.badminton.mes.module.craft.dal.redis.CraftRedisKeyConstants;
import com.badminton.mes.module.production.dal.redis.ProductionRedisKeyConstants;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** A 组生产与工艺缓存 Key、TTL 和命名空间契约测试。 @author 范家权 */
class ATeamCacheKeyContractTest {

    @Test
    void productionDetailAndSerialKeysUseSeparateNamespaces() {
        assertThat(ProductionRedisKeyConstants.workOrderDetailKey(10L))
                .isEqualTo("mes:production:work_order:10");
        assertThat(ProductionRedisKeyConstants.workOrderSerialKey("20260716"))
                .isEqualTo("mes:production:work_order_serial:20260716");
        assertThat(ProductionRedisKeyConstants.dispatchSerialKey("20260716"))
                .isEqualTo("mes:production:dispatch_serial:20260716");
    }

    @Test
    void productionSerialTtlCoversDateBoundary() {
        assertThat(ProductionRedisKeyConstants.WORK_ORDER_SERIAL_TTL).isEqualTo(Duration.ofDays(2));
        assertThat(ProductionRedisKeyConstants.DISPATCH_SERIAL_TTL).isEqualTo(Duration.ofDays(2));
        assertThat(ProductionRedisKeyConstants.WORK_ORDER_DETAIL_TTL)
                .isLessThan(ProductionRedisKeyConstants.WORK_ORDER_SERIAL_TTL);
    }

    @Test
    void craftProcessAggregateKeysNeverCollide() {
        Set<String> keys = Set.of(
                CraftRedisKeyConstants.processDetailKey(20L),
                CraftRedisKeyConstants.processSopsKey(20L),
                CraftRedisKeyConstants.processDefectReasonsKey(20L));

        assertThat(keys).hasSize(3)
                .contains("mes:craft:process:20", "mes:craft:process:20:sops",
                        "mes:craft:process:20:defect_reasons");
    }

    @Test
    void craftRouteAndProcessCachesHaveExplicitTtls() {
        assertThat(CraftRedisKeyConstants.defaultRouteKey(30L))
                .isEqualTo("mes:craft:route:default:30");
        assertThat(CraftRedisKeyConstants.DEFAULT_ROUTE_TTL).isEqualTo(Duration.ofHours(1));
        assertThat(CraftRedisKeyConstants.PROCESS_DATA_TTL).isEqualTo(Duration.ofMinutes(45));
    }
}
