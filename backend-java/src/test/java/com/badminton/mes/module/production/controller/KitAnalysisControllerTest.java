package com.badminton.mes.module.production.controller;

import java.util.List;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.AuthInterceptor;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.ShortageHandleSaveReqVO;
import com.badminton.mes.module.production.service.KitAnalysisService;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link KitAnalysisController} Web 切片测试。
 *
 * <p>Service 层 Mock，仅验证 Web 层契约：参数校验、统一响应结构与
 * HTTP 状态码映射(API-003)。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@WebMvcTest(KitAnalysisController.class)
class KitAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KitAnalysisService kitAnalysisService;

    /** Mock 掉登录鉴权拦截器：Web 切片无 Redis 会话依赖，鉴权逻辑由拦截器自身测试覆盖 */
    @MockitoBean
    private AuthInterceptor authInterceptor;

    @BeforeEach
    void permitAllRequests() throws Exception {
        when(authInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("执行齐套分析：返回 00000 与工单级齐套状态")
    void analyzeWorkOrderReturnsStatus() throws Exception {
        when(kitAnalysisService.analyzeWorkOrder(1001L)).thenReturn(3);

        mockMvc.perform(post("/api/production/kit_analysis/work_orders/1001/analyze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    @DisplayName("执行齐套分析：路径参数为非正数返回 A0400(方法级校验)")
    void analyzeWorkOrderRejectsNonPositiveId() throws Exception {
        mockMvc.perform(post("/api/production/kit_analysis/work_orders/0/analyze"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));

        verify(kitAnalysisService, never()).analyzeWorkOrder(any());
    }

    @Test
    @DisplayName("执行齐套分析：业务异常返回 400、业务错误码与用户提示(API-003)")
    void analyzeWorkOrderMapsBusinessError() throws Exception {
        when(kitAnalysisService.analyzeWorkOrder(1001L)).thenThrow(
                new ServiceException(ProductionErrorCodeConstants.KIT_ANALYSIS_MATERIAL_EMPTY));

        mockMvc.perform(post("/api/production/kit_analysis/work_orders/1001/analyze"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0440"))
                .andExpect(jsonPath("$.userTip").value("请先下达工单生成物料需求"));
    }

    @Test
    @DisplayName("欠料看板：返回列表数据")
    void shortageBoardReturnsList() throws Exception {
        when(kitAnalysisService.getShortageBoard()).thenReturn(List.of());

        mockMvc.perform(get("/api/production/kit_analysis/shortage_board"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("新增欠料处理：缺少必填字段返回 400 与 A0400")
    void createShortageHandleRejectsMissingFields() throws Exception {
        mockMvc.perform(post("/api/production/kit_analysis/shortage_handles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workOrderId": 1001
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));

        verify(kitAnalysisService, never()).createShortageHandle(any(ShortageHandleSaveReqVO.class));
    }

    @Test
    @DisplayName("新增欠料处理：合法请求返回 00000 与新记录 id")
    void createShortageHandleReturnsId() throws Exception {
        when(kitAnalysisService.createShortageHandle(any(ShortageHandleSaveReqVO.class))).thenReturn(7L);

        mockMvc.perform(post("/api/production/kit_analysis/shortage_handles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workOrderId": 1001,
                                  "materialId": 2,
                                  "handleType": 1,
                                  "handlerId": 5,
                                  "expectedArrivalDate": "2026-07-15",
                                  "handleRemark": "已联系供应商加急"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(7));
    }

    @Test
    @DisplayName("解决欠料处理：转发 Service 并返回 00000")
    void resolveShortageHandleReturnsSuccess() throws Exception {
        mockMvc.perform(put("/api/production/kit_analysis/shortage_handles/7/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(kitAnalysisService).resolveShortageHandle(7L);
    }
}
