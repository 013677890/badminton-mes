package com.badminton.mes.module.production.controller;

import com.badminton.mes.common.security.AuthInterceptor;
import com.badminton.mes.module.production.controller.vo.ProductionLineSaveReqVO;
import com.badminton.mes.module.production.controller.vo.WorkshopSaveReqVO;
import com.badminton.mes.module.production.service.ProductionLineService;
import com.badminton.mes.module.production.service.WorkshopService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 车间与产线 Controller Web 切片测试。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@WebMvcTest({WorkshopController.class, ProductionLineController.class})
class ProductionOrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkshopService workshopService;

    @MockitoBean
    private ProductionLineService productionLineService;

    @MockitoBean
    private AuthInterceptor authInterceptor;

    @BeforeEach
    void permitAllRequests() throws Exception {
        when(authInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("创建车间：合法请求返回新主键")
    void createWorkshopReturnsId() throws Exception {
        when(workshopService.createWorkshop(any(WorkshopSaveReqVO.class))).thenReturn(10L);

        mockMvc.perform(post("/api/production/workshops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workshopCode": "WS-A",
                                  "workshopName": "成型车间",
                                  "status": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(10));
    }

    @Test
    @DisplayName("创建产线：非法编码返回 A0400 且不调用 Service")
    void createProductionLineRejectsInvalidCode() throws Exception {
        mockMvc.perform(post("/api/production/production_lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lineCode": "产线 A",
                                  "lineName": "一号产线",
                                  "workshopId": 10,
                                  "standardCapacity": 5000,
                                  "status": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));

        verify(productionLineService, never())
                .createProductionLine(any(ProductionLineSaveReqVO.class));
    }

    @Test
    @DisplayName("车间分页：pageSize 超过上限返回 A0400")
    void getWorkshopPageRejectsOversizedPageSize() throws Exception {
        mockMvc.perform(get("/api/production/workshops/page")
                        .param("pageNo", "1")
                        .param("pageSize", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
    }

    @Test
    @DisplayName("删除产线：非正数路径参数返回 A0400")
    void deleteProductionLineRejectsNonPositiveId() throws Exception {
        mockMvc.perform(delete("/api/production/production_lines/-1")
                        .param("version", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
    }
}
