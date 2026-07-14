package com.badminton.mes.module.equipment.service.impl;

import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerSaveReqVO;
import com.badminton.mes.module.equipment.dal.dataobject.EquipmentManufacturerDO;
import com.badminton.mes.module.equipment.dal.repository.EquipmentManufacturerRepository;
import com.badminton.mes.module.equipment.service.EquipmentManufacturerService;
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
 * 设备制造商服务测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EquipmentManufacturerServiceImplTest {

    @Autowired
    private EquipmentManufacturerService equipmentManufacturerService;

    @Autowired
    private EquipmentManufacturerRepository equipmentManufacturerRepository;

    private EquipmentManufacturerSaveReqVO testManufacturerVO;

    @BeforeEach
    void setUp() {
        testManufacturerVO = new EquipmentManufacturerSaveReqVO();
        testManufacturerVO.setManufacturerCode("TEST_MFR_001");
        testManufacturerVO.setManufacturerName("测试制造商");
        testManufacturerVO.setContactPerson("张三");
        testManufacturerVO.setContactPhone("13800138000");
        testManufacturerVO.setContactEmail("test@example.com");
        testManufacturerVO.setAddress("北京市朝阳区测试路123号");
        testManufacturerVO.setWebsite("https://www.example.com");
        testManufacturerVO.setRemark("这是一个测试制造商");
        testManufacturerVO.setStatus(1);
    }

    @Test
    void testCreateManufacturer() {
        Long manufacturerId = equipmentManufacturerService.createManufacturer(testManufacturerVO);

        assertNotNull(manufacturerId);
        assertTrue(manufacturerId > 0);

        EquipmentManufacturerDO savedManufacturer = equipmentManufacturerRepository.findById(manufacturerId).orElse(null);
        assertNotNull(savedManufacturer);
        assertEquals(testManufacturerVO.getManufacturerCode(), savedManufacturer.getManufacturerCode());
        assertEquals(testManufacturerVO.getManufacturerName(), savedManufacturer.getManufacturerName());
        assertEquals(testManufacturerVO.getContactPerson(), savedManufacturer.getContactPerson());
        assertEquals(testManufacturerVO.getContactPhone(), savedManufacturer.getContactPhone());
        assertEquals(testManufacturerVO.getStatus(), savedManufacturer.getStatus());
    }

    @Test
    void testCreateManufacturerWithDuplicateCode() {
        equipmentManufacturerService.createManufacturer(testManufacturerVO);

        EquipmentManufacturerSaveReqVO duplicateVO = new EquipmentManufacturerSaveReqVO();
        duplicateVO.setManufacturerCode(testManufacturerVO.getManufacturerCode());
        duplicateVO.setManufacturerName("另一个制造商");
        duplicateVO.setStatus(1);

        assertThrows(IllegalArgumentException.class, () -> {
            equipmentManufacturerService.createManufacturer(duplicateVO);
        });
    }

    @Test
    void testCreateManufacturerWithInvalidEmail() {
        testManufacturerVO.setContactEmail("invalid-email");

        assertThrows(IllegalArgumentException.class, () -> {
            equipmentManufacturerService.createManufacturer(testManufacturerVO);
        });
    }

    @Test
    void testCreateManufacturerWithInvalidPhone() {
        testManufacturerVO.setContactPhone("1234567");

        assertThrows(IllegalArgumentException.class, () -> {
            equipmentManufacturerService.createManufacturer(testManufacturerVO);
        });
    }

    @Test
    void testUpdateManufacturer() {
        Long manufacturerId = equipmentManufacturerService.createManufacturer(testManufacturerVO);

        EquipmentManufacturerSaveReqVO updateVO = new EquipmentManufacturerSaveReqVO();
        updateVO.setManufacturerCode("TEST_MFR_001");
        updateVO.setManufacturerName("修改后的制造商名称");
        updateVO.setContactPerson("李四");
        updateVO.setContactPhone("13900139000");
        updateVO.setContactEmail("updated@example.com");
        updateVO.setAddress("上海市浦东新区测试路456号");
        updateVO.setWebsite("https://www.example2.com");
        updateVO.setRemark("修改后的备注");
        updateVO.setStatus(1);

        equipmentManufacturerService.updateManufacturer(manufacturerId, updateVO);

        EquipmentManufacturerDO updatedManufacturer = equipmentManufacturerRepository.findById(manufacturerId).orElse(null);
        assertNotNull(updatedManufacturer);
        assertEquals("修改后的制造商名称", updatedManufacturer.getManufacturerName());
        assertEquals("李四", updatedManufacturer.getContactPerson());
        assertEquals("13900139000", updatedManufacturer.getContactPhone());
        assertEquals("updated@example.com", updatedManufacturer.getContactEmail());
    }

    @Test
    void testUpdateManufacturerNotFound() {
        Long nonExistentId = 99999L;

        assertThrows(IllegalArgumentException.class, () -> {
            equipmentManufacturerService.updateManufacturer(nonExistentId, testManufacturerVO);
        });
    }

    @Test
    void testDeleteManufacturer() {
        Long manufacturerId = equipmentManufacturerService.createManufacturer(testManufacturerVO);

        equipmentManufacturerService.deleteManufacturer(manufacturerId);

        EquipmentManufacturerDO deletedManufacturer = equipmentManufacturerRepository.findById(manufacturerId).orElse(null);
        assertNotNull(deletedManufacturer);
        assertEquals(0, deletedManufacturer.getDeleted());
    }

    @Test
    void testDeleteManufacturerNotFound() {
        Long nonExistentId = 99999L;

        assertThrows(IllegalArgumentException.class, () -> {
            equipmentManufacturerService.deleteManufacturer(nonExistentId);
        });
    }

    @Test
    void testGetManufacturer() {
        Long manufacturerId = equipmentManufacturerService.createManufacturer(testManufacturerVO);

        EquipmentManufacturerDO manufacturer = equipmentManufacturerService.getManufacturer(manufacturerId);

        assertNotNull(manufacturer);
        assertEquals(testManufacturerVO.getManufacturerCode(), manufacturer.getManufacturerCode());
        assertEquals(testManufacturerVO.getManufacturerName(), manufacturer.getManufacturerName());
    }

    @Test
    void testGetManufacturerNotFound() {
        Long nonExistentId = 99999L;

        assertThrows(IllegalArgumentException.class, () -> {
            equipmentManufacturerService.getManufacturer(nonExistentId);
        });
    }

    @Test
    void testGetManufacturerPage() {
        for (int i = 1; i <= 5; i++) {
            EquipmentManufacturerSaveReqVO vo = new EquipmentManufacturerSaveReqVO();
            vo.setManufacturerCode("TEST_MFR_00" + i);
            vo.setManufacturerName("测试制造商" + i);
            vo.setStatus(1);
            equipmentManufacturerService.createManufacturer(vo);
        }

        EquipmentManufacturerPageReqVO pageReqVO = new EquipmentManufacturerPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);

        Page<EquipmentManufacturerDO> page = equipmentManufacturerService.getManufacturerPage(pageReqVO);

        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 5);
    }

    @Test
    void testGetManufacturerPageWithKeyword() {
        EquipmentManufacturerSaveReqVO vo1 = new EquipmentManufacturerSaveReqVO();
        vo1.setManufacturerCode("SEARCH_001");
        vo1.setManufacturerName("华为技术有限公司");
        vo1.setStatus(1);
        equipmentManufacturerService.createManufacturer(vo1);

        EquipmentManufacturerSaveReqVO vo2 = new EquipmentManufacturerSaveReqVO();
        vo2.setManufacturerCode("SEARCH_002");
        vo2.setManufacturerName("格力电器");
        vo2.setStatus(1);
        equipmentManufacturerService.createManufacturer(vo2);

        EquipmentManufacturerPageReqVO pageReqVO = new EquipmentManufacturerPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);
        pageReqVO.setKeyword("华为");

        Page<EquipmentManufacturerDO> page = equipmentManufacturerService.getManufacturerPage(pageReqVO);

        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 1);
        assertTrue(page.getContent().stream()
                .anyMatch(m -> m.getManufacturerName().contains("华为")));
    }

    @Test
    void testGetManufacturerPageWithStatus() {
        EquipmentManufacturerSaveReqVO activeVO = new EquipmentManufacturerSaveReqVO();
        activeVO.setManufacturerCode("ACTIVE_001");
        activeVO.setManufacturerName("启用制造商");
        activeVO.setStatus(1);
        equipmentManufacturerService.createManufacturer(activeVO);

        EquipmentManufacturerSaveReqVO inactiveVO = new EquipmentManufacturerSaveReqVO();
        inactiveVO.setManufacturerCode("INACTIVE_001");
        inactiveVO.setManufacturerName("禁用制造商");
        inactiveVO.setStatus(0);
        equipmentManufacturerService.createManufacturer(inactiveVO);

        EquipmentManufacturerPageReqVO pageReqVO = new EquipmentManufacturerPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);
        pageReqVO.setStatus(1);

        Page<EquipmentManufacturerDO> page = equipmentManufacturerService.getManufacturerPage(pageReqVO);

        assertNotNull(page);
        assertTrue(page.getContent().stream()
                .allMatch(m -> m.getStatus() == 1));
    }

    @Test
    void testGetAllManufacturers() {
        for (int i = 1; i <= 3; i++) {
            EquipmentManufacturerSaveReqVO vo = new EquipmentManufacturerSaveReqVO();
            vo.setManufacturerCode("ALL_00" + i);
            vo.setManufacturerName("全部制造商" + i);
            vo.setStatus(1);
            equipmentManufacturerService.createManufacturer(vo);
        }

        List<EquipmentManufacturerDO> allManufacturers = equipmentManufacturerService.getAllManufacturers();

        assertNotNull(allManufacturers);
        assertTrue(allManufacturers.size() >= 3);
    }

    @Test
    void testValidateManufacturerCode() {
        equipmentManufacturerService.createManufacturer(testManufacturerVO);

        assertThrows(IllegalArgumentException.class, () -> {
            equipmentManufacturerService.validateManufacturerCodeUnique(testManufacturerVO.getManufacturerCode(), null);
        });

        Long manufacturerId = equipmentManufacturerRepository
                .findByManufacturerCode(testManufacturerVO.getManufacturerCode()).getId();
        assertDoesNotThrow(() -> {
            equipmentManufacturerService.validateManufacturerCodeUnique(
                    testManufacturerVO.getManufacturerCode(), manufacturerId);
        });
    }

    @Test
    void testCreateManufacturerWithMinimalFields() {
        EquipmentManufacturerSaveReqVO minimalVO = new EquipmentManufacturerSaveReqVO();
        minimalVO.setManufacturerCode("MINIMAL_001");
        minimalVO.setManufacturerName("最小字段制造商");
        minimalVO.setStatus(1);

        Long manufacturerId = equipmentManufacturerService.createManufacturer(minimalVO);

        assertNotNull(manufacturerId);
        EquipmentManufacturerDO manufacturer = equipmentManufacturerRepository.findById(manufacturerId).orElse(null);
        assertNotNull(manufacturer);
        assertEquals("MINIMAL_001", manufacturer.getManufacturerCode());
    }

    @Test
    void testUpdateManufacturerStatus() {
        Long manufacturerId = equipmentManufacturerService.createManufacturer(testManufacturerVO);

        EquipmentManufacturerSaveReqVO updateVO = new EquipmentManufacturerSaveReqVO();
        updateVO.setManufacturerCode(testManufacturerVO.getManufacturerCode());
        updateVO.setManufacturerName(testManufacturerVO.getManufacturerName());
        updateVO.setStatus(0);

        equipmentManufacturerService.updateManufacturer(manufacturerId, updateVO);

        EquipmentManufacturerDO updatedManufacturer = equipmentManufacturerRepository.findById(manufacturerId).orElse(null);
        assertNotNull(updatedManufacturer);
        assertEquals(0, updatedManufacturer.getStatus());
    }

    @Test
    void testSearchByCode() {
        EquipmentManufacturerSaveReqVO vo = new EquipmentManufacturerSaveReqVO();
        vo.setManufacturerCode("CODE_SEARCH_001");
        vo.setManufacturerName("代码搜索测试");
        vo.setStatus(1);
        equipmentManufacturerService.createManufacturer(vo);

        EquipmentManufacturerPageReqVO pageReqVO = new EquipmentManufacturerPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);
        pageReqVO.setKeyword("CODE_SEARCH");

        Page<EquipmentManufacturerDO> page = equipmentManufacturerService.getManufacturerPage(pageReqVO);

        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 1);
        assertTrue(page.getContent().stream()
                .anyMatch(m -> m.getManufacturerCode().contains("CODE_SEARCH")));
    }

    @Test
    void testCreateManufacturerWithAllFields() {
        EquipmentManufacturerSaveReqVO completeVO = new EquipmentManufacturerSaveReqVO();
        completeVO.setManufacturerCode("COMPLETE_001");
        completeVO.setManufacturerName("完整字段制造商");
        completeVO.setContactPerson("王五");
        completeVO.setContactPhone("13700137000");
        completeVO.setContactEmail("complete@example.com");
        completeVO.setAddress("广东省深圳市南山区科技园");
        completeVO.setWebsite("https://www.complete-test.com");
        completeVO.setRemark("这是一个包含所有字段的测试制造商");
        completeVO.setStatus(1);

        Long manufacturerId = equipmentManufacturerService.createManufacturer(completeVO);

        assertNotNull(manufacturerId);
        EquipmentManufacturerDO manufacturer = equipmentManufacturerRepository.findById(manufacturerId).orElse(null);
        assertNotNull(manufacturer);
        assertEquals(completeVO.getManufacturerCode(), manufacturer.getManufacturerCode());
        assertEquals(completeVO.getManufacturerName(), manufacturer.getManufacturerName());
        assertEquals(completeVO.getContactPerson(), manufacturer.getContactPerson());
        assertEquals(completeVO.getContactPhone(), manufacturer.getContactPhone());
        assertEquals(completeVO.getContactEmail(), manufacturer.getContactEmail());
        assertEquals(completeVO.getAddress(), manufacturer.getAddress());
        assertEquals(completeVO.getWebsite(), manufacturer.getWebsite());
        assertEquals(completeVO.getRemark(), manufacturer.getRemark());
    }

    @Test
    void testPaginationBoundary() {
        for (int i = 1; i <= 25; i++) {
            EquipmentManufacturerSaveReqVO vo = new EquipmentManufacturerSaveReqVO();
            vo.setManufacturerCode("PAGE_TEST_" + String.format("%03d", i));
            vo.setManufacturerName("分页测试制造商" + i);
            vo.setStatus(1);
            equipmentManufacturerService.createManufacturer(vo);
        }

        EquipmentManufacturerPageReqVO page1ReqVO = new EquipmentManufacturerPageReqVO();
        page1ReqVO.setPageNo(1);
        page1ReqVO.setPageSize(10);

        Page<EquipmentManufacturerDO> page1 = equipmentManufacturerService.getManufacturerPage(page1ReqVO);
        assertNotNull(page1);
        assertTrue(page1.getTotalElements() >= 25);
        assertTrue(page1.getTotalPages() >= 3);

        EquipmentManufacturerPageReqVO page3ReqVO = new EquipmentManufacturerPageReqVO();
        page3ReqVO.setPageNo(3);
        page3ReqVO.setPageSize(10);

        Page<EquipmentManufacturerDO> page3 = equipmentManufacturerService.getManufacturerPage(page3ReqVO);
        assertNotNull(page3);
        assertTrue(page3.getContent().size() >= 5);
    }
}
