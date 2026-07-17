package com.badminton.mes.group.c;

import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.andon.controller.vo.AndonTypeSaveReqVO;
import com.badminton.mes.module.andon.convert.AndonTypeConvert;
import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigSaveReqVO;
import com.badminton.mes.module.device.convert.DeviceAccessConfigConvert;
import com.badminton.mes.module.equipment.controller.vo.EquipmentCategorySaveReqVO;
import com.badminton.mes.module.equipment.convert.EquipmentCategoryConvert;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategorySaveReqVO;
import com.badminton.mes.module.quality.convert.QualityInspectionCategoryConvert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** C 组权限/并发维度：登录上下文门禁和跨模块转换器并发安全。 @author 范家权 */
class CTeamPermissionConcurrencyTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    @Test
    void unauthenticatedCOperationsCannotReadRequiredOperator() {
        SecurityContextHolder.clear();
        assertThatThrownBy(SecurityContextHolder::getRequiredLoginUser)
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void adminContextCarriesStableOperatorIdentity() {
        LoginUser user = new LoginUser();
        user.setUserId(900L);
        user.setRoleCodes(List.of("ADMIN"));
        SecurityContextHolder.set("admin-token", user);

        assertThat(SecurityContextHolder.getRequiredLoginUserId()).isEqualTo(900L);
    }

    @Test
    void allCConvertersRemainIndependentUnderParallelCalls() {
        List<Integer> indexes = IntStream.range(0, 300).parallel().mapToObj(index -> {
            EquipmentCategorySaveReqVO category = new EquipmentCategorySaveReqVO();
            category.setCategoryCode("C-" + index);
            EquipmentCategoryConvert.toEntity(category);

            DeviceAccessConfigSaveReqVO config = new DeviceAccessConfigSaveReqVO();
            config.setConfigCode("D-" + index);
            DeviceAccessConfigConvert.toEntity(config);

            QualityInspectionCategorySaveReqVO quality = new QualityInspectionCategorySaveReqVO();
            quality.setCategoryCode("Q-" + index);
            QualityInspectionCategoryConvert.toEntity(quality);

            AndonTypeSaveReqVO andon = new AndonTypeSaveReqVO();
            andon.setTypeCode("A-" + index);
            assertThat(AndonTypeConvert.toEntity(andon).getTypeCode()).isEqualTo("A-" + index);
            return index;
        }).toList();

        assertThat(indexes).hasSize(300).doesNotHaveDuplicates();
    }
}
