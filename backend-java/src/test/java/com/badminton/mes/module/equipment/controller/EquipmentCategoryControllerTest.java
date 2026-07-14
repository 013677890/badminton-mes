package com.badminton.mes.module.equipment.controller;

import com.badminton.mes.framework.common.pojo.CommonResult;
import com.badminton.mes.framework.common.pojo.PageResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentCategoryPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentCategorySaveReqVO;
import com.badminton.mes.module.equipment.dal.dataobject.EquipmentCategoryDO;
import com.badminton.mes.module.equipment.service.EquipmentCategoryService;
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
 * 设备类别 Controller 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EquipmentCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EquipmentCategoryService equipmentCategoryService;

    private EquipmentCategorySaveReqVO testCategoryVO;

    @BeforeEach
    void setUp() {
        testCategoryVO = new EquipmentCategorySaveReqVO();
        testCategoryVO.setCategoryCode("TEST_API_001");
        testCategoryVO.setCategoryName("API测试类别");
        testCategoryVO.setParentId(null);
        testCategoryVO.setSortOrder(100);
        testCategoryVO.setRemark("这是API测试类别");
        testCategoryVO.setStatus(1);
    }

    @Test
    void testCreateCategory() throws Exception {
        mockMvc.perform(post("/api/equipment/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCategoryVO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andExpect(jsonPath("$.data").isNumber())
                .andExpect(jsonPath("$.data").value(greaterThan(0)));
    }

    @Test
    void testCreateCategoryWithInvalidData() throws Exception {
        EquipmentCategorySaveReqVO invalidVO = new EquipmentCategorySaveReqVO();
        invalidVO.setCategoryCode("");
        invalidVO.setCategoryName("");
        invalidVO.setStatus(1);

        mockMvc.perform(post("/api/equipment/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidVO)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetCategory() throws Exception {
        Long categoryId = equipmentCategoryService.createCategory(testCategoryVO);

        mockMvc.perform(get("/api/equipment/categories/{id}", categoryId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(categoryId))
                .andExpect(jsonPath("$.data.categoryCode").value(testCategoryVO.getCategoryCode()))
                .andExpect(jsonPath("$.data.categoryName").value(testCategoryVO.getCategoryName()))
                .andExpect(jsonPath("$.data.status").value(testCategoryVO.getStatus()));
    }

    @Test
    void testGetCategoryNotFound() throws Exception {
        mockMvc.perform(get("/api/equipment/categories/{id}", 99999L))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(0)));
    }

    @Test
    void testGetCategoryPage() throws Exception {
        for (int i = 1; i <= 3; i++) {
            EquipmentCategorySaveReqVO vo = new EquipmentCategorySaveReqVO();
            vo.setCategoryCode("PAGE_TEST_00" + i);
            vo.setCategoryName("分页测试类别" + i);
            vo.setStatus(1);
            equipmentCategoryService.createCategory(vo);
        }

        mockMvc.perform(get("/api/equipment/categories/page")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(3)));
    }

    @Test
    void testGetCategoryPageWithKeyword() throws Exception {
        EquipmentCategorySaveReqVO vo = new EquipmentCategorySaveReqVO();
        vo.setCategoryCode("KEYWORD_001");
        vo.setCategoryName("关键词搜索测试");
        vo.setStatus(1);
        equipmentCategoryService.createCategory(vo);

        mockMvc.perform(get("/api/equipment/categories/page")
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("keyword", "关键词"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void testGetCategoryPageWithStatus() throws Exception {
        EquipmentCategorySaveReqVO activeVO = new EquipmentCategorySaveReqVO();
        activeVO.setCategoryCode("STATUS_ACTIVE");
        activeVO.setCategoryName("启用状态测试");
        activeVO.setStatus(1);
        equipmentCategoryService.createCategory(activeVO);

        mockMvc.perform(get("/api/equipment/categories/page")
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("status", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[*].status").value(everyItem(equalTo(1))));
    }

    @Test
    void testUpdateCategory() throws Exception {
        Long categoryId = equipmentCategoryService.createCategory(testCategoryVO);

        EquipmentCategorySaveReqVO updateVO = new EquipmentCategorySaveReqVO();
        updateVO.setCategoryCode("TEST_API_001");
        updateVO.setCategoryName("修改后的API测试类别");
        updateVO.setSortOrder(200);
        updateVO.setRemark("修改后的备注");
        updateVO.setStatus(1);

        mockMvc.perform(put("/api/equipment/categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateVO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作成功"));

        mockMvc.perform(get("/api/equipment/categories/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryName").value("修改后的API测试类别"))
                .andExpect(jsonPath("$.data.sortOrder").value(200));
    }

    @Test
    void testUpdateCategoryNotFound() throws Exception {
        mockMvc.perform(put("/api/equipment/categories/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCategoryVO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(0)));
    }

    @Test
    void testDeleteCategory() throws Exception {
        Long categoryId = equipmentCategoryService.createCategory(testCategoryVO);

        mockMvc.perform(delete("/api/equipment/categories/{id}", categoryId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作成功"));

        mockMvc.perform(get("/api/equipment/categories/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(0)));
    }

    @Test
    void testDeleteCategoryNotFound() throws Exception {
        mockMvc.perform(delete("/api/equipment/categories/{id}", 99999L))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(0)));
    }

    @Test
    void testGetAllCategories() throws Exception {
        for (int i = 1; i <= 2; i++) {
            EquipmentCategorySaveReqVO vo = new EquipmentCategorySaveReqVO();
            vo.setCategoryCode("ALL_TEST_00" + i);
            vo.setCategoryName("全部查询测试" + i);
            vo.setStatus(1);
            equipmentCategoryService.createCategory(vo);
        }

        mockMvc.perform(get("/api/equipment/categories"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void testCreateCategoryWithParent() throws Exception {
        Long parentId = equipmentCategoryService.createCategory(testCategoryVO);

        EquipmentCategorySaveReqVO childVO = new EquipmentCategorySaveReqVO();
        childVO.setCategoryCode("CHILD_API_001");
        childVO.setCategoryName("子类别API测试");
        childVO.setParentId(parentId);
        childVO.setStatus(1);

        mockMvc.perform(post("/api/equipment/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childVO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testGetCategoryPageWithParentId() throws Exception {
        Long parentId = equipmentCategoryService.createCategory(testCategoryVO);

        for (int i = 1; i <= 2; i++) {
            EquipmentCategorySaveReqVO childVO = new EquipmentCategorySaveReqVO();
            childVO.setCategoryCode("PARENT_CHILD_00" + i);
            childVO.setCategoryName("父级过滤测试" + i);
            childVO.setParentId(parentId);
            childVO.setStatus(1);
            equipmentCategoryService.createCategory(childVO);
        }

        mockMvc.perform(get("/api/equipment/categories/page")
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("parentId", parentId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[*].parentId").value(everyItem(equalTo(parentId.intValue()))));
    }

    @Test
    void testContentTypeValidation() throws Exception {
        mockMvc.perform(post("/api/equipment/categories")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(objectMapper.writeValueAsString(testCategoryVO)))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void testInvalidJsonFormat() throws Exception {
        mockMvc.perform(post("/api/equipment/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPaginationParameters() throws Exception {
        mockMvc.perform(get("/api/equipment/categories/page")
                        .param("pageNo", "0")
                        .param("pageSize", "10"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/equipment/categories/page")
                        .param("pageNo", "1")
                        .param("pageSize", "0"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCompleteWorkflow() throws Exception {
        String createResponse = mockMvc.perform(post("/api/equipment/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCategoryVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();

        CommonResult<Long> createResult = objectMapper.readValue(createResponse, 
                objectMapper.getTypeFactory().constructParametricType(CommonResult.class, Long.class));
        Long categoryId = createResult.getData();

        mockMvc.perform(get("/api/equipment/categories/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.categoryName").value(testCategoryVO.getCategoryName()));

        EquipmentCategorySaveReqVO updateVO = new EquipmentCategorySaveReqVO();
        updateVO.setCategoryCode("TEST_API_001");
        updateVO.setCategoryName("完整流程测试-已修改");
        updateVO.setStatus(1);

        mockMvc.perform(put("/api/equipment/categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/equipment/categories/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryName").value("完整流程测试-已修改"));

        mockMvc.perform(delete("/api/equipment/categories/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
