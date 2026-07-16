package com.badminton.mes.module.equipment.service.impl;

import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.craft.dal.repository.CraftRouteDetailRepository;
import com.badminton.mes.module.equipment.constants.EquipmentErrorCodeConstants;
import com.badminton.mes.module.equipment.controller.vo.EquipmentCategorySaveReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentCategoryEntity;
import com.badminton.mes.module.equipment.dal.redis.EquipmentCache;
import com.badminton.mes.module.equipment.dal.repository.EquipmentCategoryRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentFaultPrincipleRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentLedgerRepository;

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
 * {@link EquipmentCategoryServiceImpl} 工序反向引用校验测试。
 *
 * <p>使用 Mockito 隔离设备、故障原理、工序、工艺路线和缓存依赖，直接调用被测 Service。覆盖设备
 * 类别停用或删除时的跨模块引用保护，尤其验证启用工序和生效路线会阻止写入，避免类别失效后留下
 * 无法执行的工艺配置。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@ExtendWith(MockitoExtension.class)
class EquipmentCategoryServiceImplTest {

    /** 测试类别主键。 */
    private static final Long CATEGORY_ID = 20L;

    @Mock
    private EquipmentCategoryRepository categoryRepository;

    @Mock
    private EquipmentLedgerRepository ledgerRepository;

    @Mock
    private EquipmentFaultPrincipleRepository faultPrincipleRepository;

    @Mock
    private CraftProcessRepository processRepository;

    @Mock
    private CraftRouteDetailRepository routeDetailRepository;

    @Mock
    private EquipmentCache equipmentCache;

    private EquipmentCategoryServiceImpl categoryService;

    /** 每个用例使用全新的 Service，保持 Mock 调用和实体状态独立。 */
    @BeforeEach
    void setUp() {
        categoryService = new EquipmentCategoryServiceImpl(
                categoryRepository,
                ledgerRepository,
                faultPrincipleRepository,
                processRepository,
                routeDetailRepository,
                equipmentCache);
    }

    @Test
    @DisplayName("停用设备类别：仍被启用工序引用时拒绝修改")
    void updateCategoryRejectsEnabledProcessReference() {
        EquipmentCategoryEntity category = buildEnabledCategory();
        when(categoryRepository.findByIdAndDeletedFalseForUpdate(CATEGORY_ID))
                .thenReturn(Optional.of(category));
        when(processRepository.existsByEquipmentCategoryIdAndStatusAndDeletedFalse(CATEGORY_ID, 1))
                .thenReturn(true);
        EquipmentCategorySaveReqVO reqVO = buildSaveReqVO(0);

        assertThatThrownBy(() -> categoryService.updateEquipmentCategory(CATEGORY_ID, reqVO))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                EquipmentErrorCodeConstants.EQUIPMENT_CATEGORY_REFERENCED_BY_PROCESS));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("删除设备类别：仍被启用工序引用时拒绝删除")
    void deleteCategoryRejectsEnabledProcessReference() {
        when(categoryRepository.findByIdAndDeletedFalseForUpdate(CATEGORY_ID))
                .thenReturn(Optional.of(buildEnabledCategory()));
        when(categoryRepository.countByParentIdAndDeletedFalse(CATEGORY_ID)).thenReturn(0L);
        when(processRepository.existsByEquipmentCategoryIdAndStatusAndDeletedFalse(CATEGORY_ID, 1))
                .thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteEquipmentCategory(CATEGORY_ID))
                .isInstanceOf(ServiceException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("停用设备类别：仍被生效路线直接引用时拒绝修改")
    void updateCategoryRejectsEffectiveRouteReference() {
        when(categoryRepository.findByIdAndDeletedFalseForUpdate(CATEGORY_ID))
                .thenReturn(Optional.of(buildEnabledCategory()));
        when(routeDetailRepository.existsEffectiveRouteByEquipmentCategoryId(CATEGORY_ID, 1))
                .thenReturn(true);

        assertThatThrownBy(() -> categoryService.updateEquipmentCategory(
                CATEGORY_ID, buildSaveReqVO(0)))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                EquipmentErrorCodeConstants.EQUIPMENT_CATEGORY_REFERENCED_BY_ROUTE));
        verify(categoryRepository, never()).save(any());
    }

    /** 构造处于启用状态、可参与停用校验的类别实体。 */
    private EquipmentCategoryEntity buildEnabledCategory() {
        EquipmentCategoryEntity category = new EquipmentCategoryEntity();
        category.setId(CATEGORY_ID);
        category.setCategoryCode("MACHINE");
        category.setStatus(1);
        return category;
    }

    /** 构造类别更新请求，状态参数用于驱动启用或停用分支。 */
    private EquipmentCategorySaveReqVO buildSaveReqVO(Integer status) {
        EquipmentCategorySaveReqVO reqVO = new EquipmentCategorySaveReqVO();
        reqVO.setCategoryCode("MACHINE");
        reqVO.setCategoryName("生产设备");
        reqVO.setSortOrder(1);
        reqVO.setStatus(status);
        return reqVO;
    }
}
