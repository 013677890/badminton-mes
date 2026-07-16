package com.badminton.mes.group.a;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.module.integration.dal.entity.UnitEntity;
import com.badminton.mes.module.integration.dal.repository.UnitRepository;
import com.badminton.mes.module.integration.service.impl.UnitServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** A 组计量单位有效性维度：写锁查询、启用状态和逻辑删除边界。 @author 范家权 */
class ATeamUnitPermissionBoundaryTest {

    @Test
    void enabledUnitPassesLockAndCheck() {
        UnitRepository repository = mock(UnitRepository.class);
        UnitEntity unit = unit(CommonStatusEnum.ENABLED.getStatus(), false);
        when(repository.findByIdAndDeletedFalseForUpdate(1L)).thenReturn(Optional.of(unit));

        assertThat(new UnitServiceImpl(repository).lockAndCheckEnabled(1L)).isTrue();
    }

    @Test
    void disabledUnitFailsWithoutTreatingItAsMissingData() {
        UnitRepository repository = mock(UnitRepository.class);
        when(repository.findByIdAndDeletedFalseForUpdate(1L))
                .thenReturn(Optional.of(unit(CommonStatusEnum.DISABLED.getStatus(), false)));

        assertThat(new UnitServiceImpl(repository).lockAndCheckEnabled(1L)).isFalse();
    }

    @Test
    void logicallyDeletedOrUnknownUnitFailsClosed() {
        UnitRepository repository = mock(UnitRepository.class);
        when(repository.findByIdAndDeletedFalseForUpdate(1L)).thenReturn(Optional.empty());

        assertThat(new UnitServiceImpl(repository).lockAndCheckEnabled(1L)).isFalse();
    }

    private static UnitEntity unit(Integer status, boolean deleted) {
        UnitEntity unit = new UnitEntity();
        unit.setStatus(status);
        unit.setDeleted(deleted);
        return unit;
    }
}
