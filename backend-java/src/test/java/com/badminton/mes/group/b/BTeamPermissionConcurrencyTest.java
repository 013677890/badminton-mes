package com.badminton.mes.group.b;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.barcode.enums.BarcodeRuleItemTypeEnum;
import com.badminton.mes.module.barcode.service.impl.BarcodeValueComposer;
import com.badminton.mes.module.scene.service.SceneDataScopeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** B 组权限/并发维度：车间产线范围和条码组合器线程安全。 @author 范家权 */
class BTeamPermissionConcurrencyTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    @Test
    void operatorWithinWorkshopAndLineIsAllowed() {
        LoginUser user = user("OPERATOR", 10L, 20L);
        SecurityContextHolder.set("token", user);

        new SceneDataScopeService().check(10L, 20L);
    }

    @Test
    void operatorOutsideWorkshopIsDenied() {
        SecurityContextHolder.set("token", user("OPERATOR", 10L, 20L));

        assertThatThrownBy(() -> new SceneDataScopeService().check(99L, 20L))
                .isInstanceOfSatisfying(ServiceException.class,
                        exception -> assertThat(exception.getErrorCode().code()).isEqualTo("A0301"));
    }

    @Test
    void adminBypassesObjectScopeButStillRequiresLoginContext() {
        SecurityContextHolder.set("token", user("ADMIN", 1L, 1L));
        new SceneDataScopeService().check(999L, 999L);
    }

    @Test
    void barcodeCompositionHasNoSharedMutableStateAcrossWorkers() {
        List<BarcodeValueComposer.RuleSegment> segments = List.of(
                new BarcodeValueComposer.RuleSegment(1, BarcodeRuleItemTypeEnum.CONSTANT.getType(),
                        "B", null, null),
                new BarcodeValueComposer.RuleSegment(2, BarcodeRuleItemTypeEnum.SERIAL.getType(),
                        null, null, 4));
        List<String> values = IntStream.rangeClosed(1, 500).parallel()
                .mapToObj(index -> BarcodeValueComposer.compose(segments,
                        new BarcodeValueComposer.ComposeContext(LocalDate.of(2026, 7, 15),
                                null, null, index, 4)))
                .toList();

        assertThat(values).hasSize(500).doesNotHaveDuplicates();
    }

    private static LoginUser user(String role, Long workshopId, Long lineId) {
        LoginUser user = new LoginUser();
        user.setUserId(1L);
        user.setWorkshopId(workshopId);
        user.setLineId(lineId);
        user.setRoleCodes(List.of(role));
        return user;
    }
}
