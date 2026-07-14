package com.badminton.mes.module.production.controller;

import java.util.List;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.AuthInterceptor;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.DispatchSaveReqVO;
import com.badminton.mes.module.production.service.DispatchOrderService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link DispatchOrderController} Web 切片测试。
 *
 * <p>Service 层 Mock，仅验证 Web 层契约：参数校验、统一响应结构与
 * HTTP 状态码映射(API-003)。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@WebMvcTest(DispatchOrderController.class)
class DispatchOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DispatchOrderService dispatchOrderService;

    /** Mock 掉登录鉴权拦截器：Web 切片无 Redis 会话依赖，鉴权逻辑由拦截器自身测试覆盖 */
    @MockitoBean
    private AuthInterceptor authInterceptor;

    @BeforeEach
    void permitAllRequests() throws Exception {
        when(authInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("创建派工单：合法请求返回 00000 与新派工单 id")
    void createDispatchReturnsId() throws Exception {
        when(dispatchOrderService.createDispatch(any(DispatchSaveReqVO.class))).thenReturn(51L);

        mockMvc.perform(post("/api/production/dispatch_orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workOrderId": 1001,
                                  "lineId": 3,
                                  "shiftId": 1,
                                  "planDate": "2026-07-15",
                                  "planQuantity": 2000,
                                  "planStartTime": "2026-07-15 08:00:00",
                                  "planEndTime": "2026-07-15 20:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(51));
    }

    @Test
    @DisplayName("创建派工单：缺少必填字段返回 400 与 A0400")
    void createDispatchRejectsMissingFields() throws Exception {
        mockMvc.perform(post("/api/production/dispatch_orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workOrderId": 1001
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));

        verify(dispatchOrderService, never()).createDispatch(any());
    }

    @Test
    @DisplayName("创建派工单：业务超派异常返回 400 与 A0420(API-003)")
    void createDispatchMapsBusinessError() throws Exception {
        when(dispatchOrderService.createDispatch(any(DispatchSaveReqVO.class))).thenThrow(
                new ServiceException(ProductionErrorCodeConstants.DISPATCH_QUANTITY_EXCEED));

        mockMvc.perform(post("/api/production/dispatch_orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workOrderId": 1001,
                                  "lineId": 3,
                                  "shiftId": 1,
                                  "planDate": "2026-07-15",
                                  "planQuantity": 99999,
                                  "planStartTime": "2026-07-15 08:00:00",
                                  "planEndTime": "2026-07-15 20:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0420"));
    }

    @Test
    @DisplayName("排产建议：work_order_id 查询参数绑定并返回列表")
    void suggestDispatchBindsQueryParam() throws Exception {
        when(dispatchOrderService.suggestDispatch(1001L)).thenReturn(List.of());

        mockMvc.perform(get("/api/production/dispatch_orders/suggest")
                        .param("work_order_id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("分页查询：pageSize 超过上限 100 返回 A0400(FLOW-012 入参保护)")
    void pageRejectsOversizedPageSize() throws Exception {
        mockMvc.perform(get("/api/production/dispatch_orders/page")
                        .param("pageNo", "1")
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
    }

    @Test
    @DisplayName("审核派工单：转发 Service 并返回 00000")
    void auditDispatchForwardsToService() throws Exception {
        mockMvc.perform(put("/api/production/dispatch_orders/51/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(dispatchOrderService).auditDispatch(51L);
    }

    @Test
    @DisplayName("取消派工单：原因为空白返回 A0400 且不调用 Service")
    void cancelDispatchRejectsBlankReason() throws Exception {
        mockMvc.perform(put("/api/production/dispatch_orders/51/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));

        verify(dispatchOrderService, never()).cancelDispatch(any(), anyString());
    }

    @Test
    @DisplayName("取消派工单：合法请求转发原因给 Service")
    void cancelDispatchForwardsReason() throws Exception {
        mockMvc.perform(put("/api/production/dispatch_orders/51/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "计划变更"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(dispatchOrderService).cancelDispatch(eq(51L), eq("计划变更"));
    }

    @Test
    @DisplayName("产线排程：日期参数按 yyyy-MM-dd 绑定")
    void scheduleBindsDateParams() throws Exception {
        when(dispatchOrderService.getLineSchedule(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/production/dispatch_orders/schedule")
                        .param("line_id", "3")
                        .param("start_date", "2026-07-13")
                        .param("end_date", "2026-07-19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isArray());
    }
}
