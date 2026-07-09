package com.badminton.mes.module.equipment.service.impl;

import com.badminton.mes.module.equipment.controller.vo.EquipmentCategoryPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentCategorySaveReqVO;
import com.badminton.mes.module.equipment.dal.dataobject.EquipmentCategoryDO;
import com.badminton.mes.module.equipment.dal.repository.EquipmentCategoryRepository;
import com.badminton.mes.module.equipment.service.EquipmentCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 设备类别服务测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EquipmentCategoryServiceImplTest {

    @Autowired
    private EquipmentCategoryService equipmentCategoryService;

    @Autowired
    private EquipmentCategoryRepository equipmentCategoryRepository;

    private EquipmentCategorySaveReqVO testCategoryVO;

    @BeforeEach
    void setUp() {
        testCategoryVO = new EquipmentCategorySaveReqVO();
        testCategoryVO.setCategoryCode("TEST_CAT_001");
        testCategoryVO.setCategoryName("测试设备类别");
        testCategoryVO.setParentId(null);
        testCategoryVO.setSortOrder(100);
        testCategoryVO.setRemark("这是一个测试类别");
        testCategoryVO.setStatus(1);
    }

    @Test
    void testCreateCategory() {
        Long categoryId = equipmentCategoryService.createCategory(testCategoryVO);

        assertNotNull(categoryId);
        assertTrue(categoryId > 0);

        EquipmentCategoryDO savedCategory = equipmentCategoryRepository.findById(categoryId).orElse(null);
        assertNotNull(savedCategory);
        assertEquals(testCategoryVO.getCategoryCode(), savedCategory.getCategoryCode());
        assertEquals(testCategoryVO.getCategoryName(), savedCategory.getCategoryName());
        assertEquals(testCategoryVO.getStatus(), savedCategory.getStatus());
    }

    @Test
    void testCreateCategoryWithDuplicateCode() {
        equipmentCategoryService.createCategory(testCategoryVO);

        EquipmentCategorySaveReqVO duplicateVO = new EquipmentCategorySaveReqVO();
        duplicateVO.setCategoryCode(testCategoryVO.getCategoryCode());
        duplicateVO.setCategoryName("另一个类别");
        duplicateVO.setStatus(1);

        assertThrows(IllegalArgumentException.class, () -> {
            equipmentCategoryService.createCategory(duplicateVO);
        });
    }

    @Test
    void testUpdateCategory() {
        Long categoryId = equipmentCategoryService.createCategory(testCategoryVO);

        EquipmentCategorySaveReqVO updateVO = new EquipmentCategorySaveReqVO();
        updateVO.setCategoryCode("TEST_CAT_001");
        updateVO.setCategoryName("修改后的类别名称");
        updateVO.setSortOrder(200);
        updateVO.setRemark("修改后的备注");
        updateVO.setStatus(1);

        equipmentCategoryService.updateCategory(categoryId, updateVO);

        EquipmentCategoryDO updatedCategory = equipmentCategoryRepository.findById(categoryId).orElse(null);
        assertNotNull(updatedCategory);
        assertEquals("修改后的类别名称", updatedCategory.getCategoryName());
        assertEquals(200, updatedCategory.getSortOrder());
        assertEquals("修改后的备注", updatedCategory.getRemark());
    }

    @Test
    void testUpdateCategoryNotFound() {
        Long nonExistentId = 99999L;

        assertThrows(IllegalArgumentException.class, () -> {
            equipmentCategoryService.updateCategory(nonExistentId, testCategoryVO);
        });
    }

    @Test
    void testDeleteCategory() {
        Long categoryId = equipmentCategoryService.createCategory(testCategoryVO);

        equipmentCategoryService.deleteCategory(categoryId);

        EquipmentCategoryDO deletedCategory = equipmentCategoryRepository.findById(categoryId).orElse(null);
        assertNotNull(deletedCategory);
        assertEquals(0, deletedCategory.getDeleted());
    }

    @Test
    void testDeleteCategoryWithChildren() {
        Long parentId = equipmentCategoryService.createCategory(testCategoryVO);

        EquipmentCategorySaveReqVO childVO = new EquipmentCategorySaveReqVO();
        childVO.setCategoryCode("TEST_CAT_002");
        childVO.setCategoryName("子类别");
        childVO.setParentId(parentId);
        childVO.setStatus(1);
        equipmentCategoryService.createCategory(childVO);

        assertThrows(IllegalStateException.class, () -> {
            equipmentCategoryService.deleteCategory(parentId);
        });
    }

    @Test
    void testGetCategory() {
        Long categoryId = equipmentCategoryService.createCategory(testCategoryVO);

        EquipmentCategoryDO category = equipmentCategoryService.getCategory(categoryId);

        assertNotNull(category);
        assertEquals(testCategoryVO.getCategoryCode(), category.getCategoryCode());
        assertEquals(testCategoryVO.getCategoryName(), category.getCategoryName());
    }

    @Test
    void testGetCategoryNotFound() {
        Long nonExistentId = 99999L;

        assertThrows(IllegalArgumentException.class, () -> {
            equipmentCategoryService.getCategory(nonExistentId);
        });
    }

    @Test
    void testGetCategoryPage() {
        for (int i = 1; i <= 5; i++) {
            EquipmentCategorySaveReqVO vo = new EquipmentCategorySaveReqVO();
            vo.setCategoryCode("TEST_CAT_00" + i);
            vo.setCategoryName("测试类别" + i);
            vo.setStatus(1);
            equipmentCategoryService.createCategory(vo);
        }

        EquipmentCategoryPageReqVO pageReqVO = new EquipmentCategoryPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);

        Page<EquipmentCategoryDO> page = equipmentCategoryService.getCategoryPage(pageReqVO);

        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 5);
    }

    @Test
    void testGetCategoryPageWithKeyword() {
        EquipmentCategorySaveReqVO vo1 = new EquipmentCategorySaveReqVO();
        vo1.setCategoryCode("SEARCH_001");
        vo1.setCategoryName("机械加工设备");
        vo1.setStatus(1);
        equipmentCategoryService.createCategory(vo1);

        EquipmentCategorySaveReqVO vo2 = new EquipmentCategorySaveReqVO();
        vo2.setCategoryCode("SEARCH_002");
        vo2.setCategoryName("电气设备");
        vo2.setStatus(1);
        equipmentCategoryService.createCategory(vo2);

        EquipmentCategoryPageReqVO pageReqVO = new EquipmentCategoryPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);
        pageReqVO.setKeyword("机械");

        Page<EquipmentCategoryDO> page = equipmentCategoryService.getCategoryPage(pageReqVO);

        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 1);
        assertTrue(page.getContent().stream()
                .anyMatch(c -> c.getCategoryName().contains("机械")));
    }

    @Test
    void testGetCategoryPageWithParentId() {
        Long parentId = equipmentCategoryService.createCategory(testCategoryVO);

        for (int i = 1; i <= 3; i++) {
            EquipmentCategorySaveReqVO childVO = new EquipmentCategorySaveReqVO();
            childVO.setCategoryCode("CHILD_00" + i);
            childVO.setCategoryName("子类别" + i);
            childVO.setParentId(parentId);
            childVO.setStatus(1);
            equipmentCategoryService.createCategory(childVO);
        }

        EquipmentCategoryPageReqVO pageReqVO = new EquipmentCategoryPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);
        pageReqVO.setParentId(parentId);

        Page<EquipmentCategoryDO> page = equipmentCategoryService.getCategoryPage(pageReqVO);

        assertNotNull(page);
        assertEquals(3, page.getTotalElements());
        assertTrue(page.getContent().stream()
                .allMatch(c -> c.getParentId().equals(parentId)));
    }

    @Test
    void testGetCategoryPageWithStatus() {
        EquipmentCategorySaveReqVO activeVO = new EquipmentCategorySaveReqVO();
        activeVO.setCategoryCode("ACTIVE_001");
        activeVO.setCategoryName("启用类别");
        activeVO.setStatus(1);
        equipmentCategoryService.createCategory(activeVO);

        EquipmentCategorySaveReqVO inactiveVO = new EquipmentCategorySaveReqVO();
        inactiveVO.setCategoryCode("INACTIVE_001");
        inactiveVO.setCategoryName("禁用类别");
        inactiveVO.setStatus(0);
        equipmentCategoryService.createCategory(inactiveVO);

        EquipmentCategoryPageReqVO pageReqVO = new EquipmentCategoryPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);
        pageReqVO.setStatus(1);

        Page<EquipmentCategoryDO> page = equipmentCategoryService.getCategoryPage(pageReqVO);

        assertNotNull(page);
        assertTrue(page.getContent().stream()
                .allMatch(c -> c.getStatus() == 1));
    }

    @Test
    void testGetAllCategories() {
        for (int i = 1; i <= 3; i++) {
            EquipmentCategorySaveReqVO vo = new EquipmentCategorySaveReqVO();
            vo.setCategoryCode("ALL_00" + i);
            vo.setCategoryName("全部类别" + i);
            vo.setStatus(1);
            equipmentCategoryService.createCategory(vo);
        }

        List<EquipmentCategoryDO> allCategories = equipmentCategoryService.getAllCategories();

        assertNotNull(allCategories);
        assertTrue(allCategories.size() >= 3);
    }

    @Test
    void testValidateCategoryCode() {
        equipmentCategoryService.createCategory(testCategoryVO);

        assertThrows(IllegalArgumentException.class, () -> {
            equipmentCategoryService.validateCategoryCodeUnique(testCategoryVO.getCategoryCode(), null);
        });

        Long categoryId = equipmentCategoryRepository.findByCategoryCode(testCategoryVO.getCategoryCode()).getId();
        assertDoesNotThrow(() -> {
            equipmentCategoryService.validateCategoryCodeUnique(testCategoryVO.getCategoryCode(), categoryId);
        });
    }

    @Test
    void testCategoryHierarchy() {
        Long level1Id = equipmentCategoryService.createCategory(testCategoryVO);

        EquipmentCategorySaveReqVO level2VO = new EquipmentCategorySaveReqVO();
        level2VO.setCategoryCode("LEVEL2_001");
        level2VO.setCategoryName("二级类别");
        level2VO.setParentId(level1Id);
        level2VO.setStatus(1);
        Long level2Id = equipmentCategoryService.createCategory(level2VO);

        EquipmentCategorySaveReqVO level3VO = new EquipmentCategorySaveReqVO();
        level3VO.setCategoryCode("LEVEL3_001");
        level3VO.setCategoryName("三级类别");
        level3VO.setParentId(level2Id);
        level3VO.setStatus(1);
        Long level3Id = equipmentCategoryService.createCategory(level3VO);

        EquipmentCategoryDO level3Category = equipmentCategoryRepository.findById(level3Id).orElse(null);
        assertNotNull(level3Category);
        assertEquals(level2Id, level3Category.getParentId());

        EquipmentCategoryDO level2Category = equipmentCategoryRepository.findById(level2Id).orElse(null);
        assertNotNull(level2Category);
        assertEquals(level1Id, level2Category.getParentId());
    }
}
