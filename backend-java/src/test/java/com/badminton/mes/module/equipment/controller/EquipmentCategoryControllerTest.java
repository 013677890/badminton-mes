package com.badminton.mes.module.equipment.controller;

import com.badminton.mes.common.security.AuthInterceptor;
import com.badminton.mes.module.equipment.controller.vo.EquipmentCategoryPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentCategoryRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentCategorySaveReqVO;
import com.badminton.mes.module.equipment.service.EquipmentCategoryService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import com.badminton.mes.common.core.PageResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link EquipmentCategoryController} Web 切片测试。
 *
 * <p>参照生产模块 Controller 测试风格，仅验证 Web 层契约：参数校验、统一响应结构、
 * 路径转发和非法请求不进入 Service。
 */
@WebMvcTest(EquipmentCategoryController.class)
class EquipmentCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EquipmentCategoryService categoryService;

    @MockitoBean
    private AuthInterceptor authInterceptor;

    @BeforeEach
    void permitAllRequests() throws Exception {
        when(authInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("创建设备类别：合法请求返回 00000 与新类别 id")
    void createEquipmentCategoryReturnsId() throws Exception {
        when(categoryService.createEquipmentCategory(any(EquipmentCategorySaveReqVO.class))).thenReturn(100L);

        mockMvc.perform(post("/api/equipment/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryCode": "MACHINE",
                                  "categoryName": "生产设备",
                                  "sortOrder": 10,
                                  "status": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(100));
    }

    @Test
    @DisplayName("创建设备类别：缺少编码返回 A0400 且不调用 Service")
    void createEquipmentCategoryRejectsBlankCode() throws Exception {
        mockMvc.perform(post("/api/equipment/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryCode": " ",
                                  "categoryName": "生产设备",
                                  "status": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));

        verify(categoryService, never()).createEquipmentCategory(any(EquipmentCategorySaveReqVO.class));
    }

    @Test
    @DisplayName("修改设备类别：合法请求转发路径主键并返回 00000")
    void updateEquipmentCategoryForwardsPathId() throws Exception {
        mockMvc.perform(put("/api/equipment/categories/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryCode": "MACHINE",
                                  "categoryName": "生产设备",
                                  "sortOrder": 20,
                                  "status": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(categoryService).updateEquipmentCategory(any(), any(EquipmentCategorySaveReqVO.class));
    }

    @Test
    @DisplayName("删除设备类别：非正数路径参数返回 A0400")
    void deleteEquipmentCategoryRejectsNonPositiveId() throws Exception {
        mockMvc.perform(delete("/api/equipment/categories/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));

        verify(categoryService, never()).deleteEquipmentCategory(any());
    }

    @Test
    @DisplayName("查询设备类别详情：返回 Service 响应数据")
    void getEquipmentCategoryReturnsServiceResult() throws Exception {
        EquipmentCategoryRespVO response = new EquipmentCategoryRespVO();
        response.setId(100L);
        response.setCategoryCode("MACHINE");
        response.setCategoryName("生产设备");
        response.setStatus(1);
        when(categoryService.getEquipmentCategory(100L)).thenReturn(response);

        mockMvc.perform(get("/api/equipment/categories/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.categoryCode").value("MACHINE"));
    }

    @Test
    @DisplayName("设备类别分页：pageSize 超过上限返回 A0400")
    void getEquipmentCategoryPageRejectsOversizedPageSize() throws Exception {
        mockMvc.perform(get("/api/equipment/categories/page")
                        .param("pageNo", "1")
                        .param("pageSize", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));

        verify(categoryService, never()).getEquipmentCategoryPage(any(EquipmentCategoryPageReqVO.class));
    }

    @Test
    @DisplayName("设备类别分页：合法查询返回分页结构")
    void getEquipmentCategoryPageReturnsPageResult() throws Exception {
        EquipmentCategoryRespVO response = new EquipmentCategoryRespVO();
        response.setId(100L);
        response.setCategoryCode("MACHINE");
        response.setCategoryName("生产设备");
        when(categoryService.getEquipmentCategoryPage(any(EquipmentCategoryPageReqVO.class)))
                .thenReturn(PageResult.of(List.of(response), 1L, 1, 20));

        mockMvc.perform(get("/api/equipment/categories/page")
                        .param("pageNo", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].categoryCode").value("MACHINE"));
    }
}
