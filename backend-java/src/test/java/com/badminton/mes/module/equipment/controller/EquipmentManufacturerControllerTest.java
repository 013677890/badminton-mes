package com.badminton.mes.module.equipment.controller;

import com.badminton.mes.framework.common.pojo.CommonResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerSaveReqVO;
import com.badminton.mes.module.equipment.service.EquipmentManufacturerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 设备制造商 Controller 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EquipmentManufacturerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EquipmentManufacturerService equipmentManufacturerService;

    private EquipmentManufacturerSaveReqVO testManufacturerVO;

    @BeforeEach
    void setUp() {
        testManufacturerVO = new EquipmentManufacturerSaveReqVO();
        testManufacturerVO.setManufacturerCode("TEST_MFR_API_001");
        testManufacturerVO.setManufacturerName("API测试制造商");
        testManufacturerVO.setContactPerson("张三");
        testManufacturerVO.setContactPhone("13800138000");
        testManufacturerVO.setContactEmail("test@example.com");
        testManufacturerVO.setAddress("北京市朝阳区测试路123号");
        testManufacturerVO.setWebsite("https://www.example.com");
        testManufacturerVO.setRemark("这是API测试制造商");
        testManufacturerVO.setStatus(1);
    }

    @Test
    void testCreateManufacturer() throws Exception {
        mockMvc.perform(post("/api/equipment/manufacturers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testManufacturerVO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andExpect(jsonPath("$.data").isNumber())
                .andExpect(jsonPath("$.data").value(greaterThan(0)));
    }

    @Test
    void testCreateManufacturerWithInvalidData() throws Exception {
        EquipmentManufacturerSaveReqVO invalidVO = new EquipmentManufacturerSaveReqVO();
        invalidVO.setManufacturerCode("");
        invalidVO.setManufacturerName("");
        invalidVO.setStatus(1);

        mockMvc.perform(post("/api/equipment/manufacturers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidVO)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetManufacturer() throws Exception {
        Long manufacturerId = equipmentManufacturerService.createManufacturer(testManufacturerVO);

        mockMvc.perform(get("/api/equipment/manufacturers/{id}", manufacturerId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(manufacturerId))
                .andExpect(jsonPath("$.data.manufacturerCode").value(testManufacturerVO.getManufacturerCode()))
                .andExpect(jsonPath("$.data.manufacturerName").value(testManufacturerVO.getManufacturerName()))
                .andExpect(jsonPath("$.data.contactPerson").value(testManufacturerVO.getContactPerson()))
                .andExpect(jsonPath("$.data.status").value(testManufacturerVO.getStatus()));
    }

    @Test
    void testGetManufacturerNotFound() throws Exception {
        mockMvc.perform(get("/api/equipment/manufacturers/{id}", 99999L))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(0)));
    }

    @Test
    void testGetManufacturerPage() throws Exception {
        for (int i = 1; i <= 3; i++) {
            EquipmentManufacturerSaveReqVO vo = new EquipmentManufacturerSaveReqVO();
            vo.setManufacturerCode("PAGE_MFR_00" + i);
            vo.setManufacturerName("分页测试制造商" + i);
            vo.setStatus(1);
            equipmentManufacturerService.createManufacturer(vo);
        }

        mockMvc.perform(get("/api/equipment/manufacturers/page")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(3)));
    }

    @Test
    void testGetManufacturerPageWithKeyword() throws Exception {
        EquipmentManufacturerSaveReqVO vo = new EquipmentManufacturerSaveReqVO();
        vo.setManufacturerCode("KEYWORD_MFR_001");
        vo.setManufacturerName("华为技术有限公司");
        vo.setStatus(1);
        equipmentManufacturerService.createManufacturer(vo);

        mockMvc.perform(get("/api/equipment/manufacturers/page")
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("keyword", "华为"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void testGetManufacturerPageWithStatus() throws Exception {
        EquipmentManufacturerSaveReqVO activeVO = new EquipmentManufacturerSaveReqVO();
        activeVO.setManufacturerCode("STATUS_ACTIVE_MFR");
        activeVO.setManufacturerName("启用状态制造商");
        activeVO.setStatus(1);
        equipmentManufacturerService.createManufacturer(activeVO);

        mockMvc.perform(get("/api/equipment/manufacturers/page")
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("status", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[*].status").value(everyItem(equalTo(1))));
    }

    @Test
    void testUpdateManufacturer() throws Exception {
        Long manufacturerId = equipmentManufacturerService.createManufacturer(testManufacturerVO);

        EquipmentManufacturerSaveReqVO updateVO = new EquipmentManufacturerSaveReqVO();
        updateVO.setManufacturerCode("TEST_MFR_API_001");
        updateVO.setManufacturerName("修改后的API测试制造商");
        updateVO.setContactPerson("李四");
        updateVO.setContactPhone("13900139000");
        updateVO.setContactEmail("updated@example.com");
        updateVO.setAddress("上海市浦东新区测试路456号");
        updateVO.setWebsite("https://www.updated.com");
        updateVO.setRemark("修改后的备注");
        updateVO.setStatus(1);

        mockMvc.perform(put("/api/equipment/manufacturers/{id}", manufacturerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateVO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作成功"));

        mockMvc.perform(get("/api/equipment/manufacturers/{id}", manufacturerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.manufacturerName").value("修改后的API测试制造商"))
                .andExpect(jsonPath("$.data.contactPerson").value("李四"));
    }

    @Test
    void testUpdateManufacturerNotFound() throws Exception {
        mockMvc.perform(put("/api/equipment/manufacturers/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testManufacturerVO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(0)));
    }

    @Test
    void testDeleteManufacturer() throws Exception {
        Long manufacturerId = equipmentManufacturerService.createManufacturer(testManufacturerVO);

        mockMvc.perform(delete("/api/equipment/manufacturers/{id}", manufacturerId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作成功"));

        mockMvc.perform(get("/api/equipment/manufacturers/{id}", manufacturerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(0)));
    }

    @Test
    void testDeleteManufacturerNotFound() throws Exception {
        mockMvc.perform(delete("/api/equipment/manufacturers/{id}", 99999L))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(0)));
    }

    @Test
    void testGetAllManufacturers() throws Exception {
        for (int i = 1; i <= 2; i++) {
            EquipmentManufacturerSaveReqVO vo = new EquipmentManufacturerSaveReqVO();
            vo.setManufacturerCode("ALL_MFR_00" + i);
            vo.setManufacturerName("全部查询测试" + i);
            vo.setStatus(1);
            equipmentManufacturerService.createManufacturer(vo);
        }

        mockMvc.perform(get("/api/equipment/manufacturers"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void testCreateManufacturerWithMinimalFields() throws Exception {
        EquipmentManufacturerSaveReqVO minimalVO = new EquipmentManufacturerSaveReqVO();
        minimalVO.setManufacturerCode("MINIMAL_MFR");
        minimalVO.setManufacturerName("最小字段制造商");
        minimalVO.setStatus(1);

        mockMvc.perform(post("/api/equipment/manufacturers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(minimalVO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testCreateManufacturerWithInvalidEmail() throws Exception {
        testManufacturerVO.setContactEmail("invalid-email-format");

        mockMvc.perform(post("/api/equipment/manufacturers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testManufacturerVO)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateManufacturerWithInvalidPhone() throws Exception {
        testManufacturerVO.setContactPhone("123");

        mockMvc.perform(post("/api/equipment/manufacturers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testManufacturerVO)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testContentTypeValidation() throws Exception {
        mockMvc.perform(post("/api/equipment/manufacturers")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(objectMapper.writeValueAsString(testManufacturerVO)))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void testInvalidJsonFormat() throws Exception {
        mockMvc.perform(post("/api/equipment/manufacturers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPaginationParameters() throws Exception {
        mockMvc.perform(get("/api/equipment/manufacturers/page")
                        .param("pageNo", "0")
                        .param("pageSize", "10"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/equipment/manufacturers/page")
                        .param("pageNo", "1")
                        .param("pageSize", "0"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchByManufacturerCode() throws Exception {
        EquipmentManufacturerSaveReqVO vo = new EquipmentManufacturerSaveReqVO();
        vo.setManufacturerCode("SEARCH_CODE_001");
        vo.setManufacturerName("代码搜索测试");
        vo.setStatus(1);
        equipmentManufacturerService.createManufacturer(vo);

        mockMvc.perform(get("/api/equipment/manufacturers/page")
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("keyword", "SEARCH_CODE"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void testCompleteWorkflow() throws Exception {
        String createResponse = mockMvc.perform(post("/api/equipment/manufacturers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testManufacturerVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();

        CommonResult<Long> createResult = objectMapper.readValue(createResponse,
                objectMapper.getTypeFactory().constructParametricType(CommonResult.class, Long.class));
        Long manufacturerId = createResult.getData();

        mockMvc.perform(get("/api/equipment/manufacturers/{id}", manufacturerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.manufacturerName").value(testManufacturerVO.getManufacturerName()));

        EquipmentManufacturerSaveReqVO updateVO = new EquipmentManufacturerSaveReqVO();
        updateVO.setManufacturerCode("TEST_MFR_API_001");
        updateVO.setManufacturerName("完整流程测试-已修改");
        updateVO.setStatus(1);

        mockMvc.perform(put("/api/equipment/manufacturers/{id}", manufacturerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/equipment/manufacturers/{id}", manufacturerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.manufacturerName").value("完整流程测试-已修改"));

        mockMvc.perform(delete("/api/equipment/manufacturers/{id}", manufacturerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testCreateMultipleManufacturers() throws Exception {
        for (int i = 1; i <= 5; i++) {
            EquipmentManufacturerSaveReqVO vo = new EquipmentManufacturerSaveReqVO();
            vo.setManufacturerCode("BATCH_MFR_00" + i);
            vo.setManufacturerName("批量创建测试" + i);
            vo.setStatus(1);

            mockMvc.perform(post("/api/equipment/manufacturers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(vo)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        mockMvc.perform(get("/api/equipment/manufacturers/page")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(5)));
    }

    @Test
    void testUpdateManufacturerPartialFields() throws Exception {
        Long manufacturerId = equipmentManufacturerService.createManufacturer(testManufacturerVO);

        EquipmentManufacturerSaveReqVO partialUpdateVO = new EquipmentManufacturerSaveReqVO();
        partialUpdateVO.setManufacturerCode(testManufacturerVO.getManufacturerCode());
        partialUpdateVO.setManufacturerName("仅更新名称");
        partialUpdateVO.setStatus(testManufacturerVO.getStatus());

        mockMvc.perform(put("/api/equipment/manufacturers/{id}", manufacturerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialUpdateVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/equipment/manufacturers/{id}", manufacturerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.manufacturerName").value("仅更新名称"));
    }
}
