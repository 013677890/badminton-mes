package com.badminton.mes.module.production.controller;

import java.time.LocalDateTime;
import java.util.List;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.AuthInterceptor;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.WorkOrderRespVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderSaveReqVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderStatusLogRespVO;
import com.badminton.mes.module.production.service.WorkOrderService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link WorkOrderController} Web 切片测试。
 *
 * <p>Service 层 Mock，仅验证 Web 层契约：参数校验、统一响应结构、
 * HTTP 状态码映射与时间格式(API-003/API-013)。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
@WebMvcTest(WorkOrderController.class)
class WorkOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkOrderService workOrderService;

    /** Mock 掉登录鉴权拦截器：Web 切片无 Redis 会话依赖，鉴权逻辑由拦截器自身测试覆盖 */
    @MockitoBean
    private AuthInterceptor authInterceptor;

    @BeforeEach
    void permitAllRequests() throws Exception {
        when(authInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("CORS 预检：允许本地前端携带 Authorization 请求头")
    void corsPreflightAllowsConfiguredFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/production/work_orders/page")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Headers",
                        org.hamcrest.Matchers.containsStringIgnoringCase("Authorization")));
    }

    @Test
    @DisplayName("CORS 预检：拒绝未配置的来源")
    void corsPreflightRejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/production/work_orders/page")
                        .header("Origin", "https://untrusted.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("创建工单：合法请求返回 00000 与新工单 id")
    void createWorkOrderReturnsId() throws Exception {
        when(workOrderService.createWorkOrder(any(WorkOrderSaveReqVO.class))).thenReturn(100L);

        mockMvc.perform(post("/api/production/work_orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 10,
                                  "workshopId": 20,
                                  "planQuantity": 1000,
                                  "planStartTime": "2026-07-10 08:00:00",
                                  "planEndTime": "2026-07-15 18:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(100));
    }

    @Test
    @DisplayName("创建工单：缺少必填字段返回 400 与 A0400，message 指明字段")
    void createWorkOrderRejectsMissingProductId() throws Exception {
        mockMvc.perform(post("/api/production/work_orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workshopId": 20,
                                  "planQuantity": 1000,
                                  "planStartTime": "2026-07-10 08:00:00",
                                  "planEndTime": "2026-07-15 18:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("productId")));
    }

    @Test
    @DisplayName("查询详情：时间字段按 yyyy-MM-dd HH:mm:ss 输出(API-013)")
    void getWorkOrderFormatsDateTime() throws Exception {
        WorkOrderRespVO respVO = new WorkOrderRespVO();
        respVO.setId(100L);
        respVO.setWorkOrderNo("WO202607080001");
        respVO.setPlanStartTime(LocalDateTime.of(2026, 7, 10, 8, 0, 0));
        when(workOrderService.getWorkOrder(100L)).thenReturn(respVO);

        mockMvc.perform(get("/api/production/work_orders/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.workOrderNo").value("WO202607080001"))
                .andExpect(jsonPath("$.data.planStartTime").value("2026-07-10 08:00:00"));
    }

    @Test
    @DisplayName("查询详情：业务异常返回 400、业务错误码与用户提示(API-003)")
    void getWorkOrderReturnsBusinessError() throws Exception {
        when(workOrderService.getWorkOrder(999L))
                .thenThrow(new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_NOT_EXISTS));

        mockMvc.perform(get("/api/production/work_orders/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0402"))
                .andExpect(jsonPath("$.userTip").isNotEmpty());
    }

    @Test
    @DisplayName("分页查询：pageSize 超过上限 100 返回 A0400(FLOW-012 入参保护)")
    void getWorkOrderPageRejectsOversizedPageSize() throws Exception {
        mockMvc.perform(get("/api/production/work_orders/page")
                        .param("pageNo", "1")
                        .param("pageSize", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
    }

    @Test
    @DisplayName("删除工单：路径参数为非正数返回 A0400(方法级校验)")
    void deleteWorkOrderRejectsNonPositiveId() throws Exception {
        mockMvc.perform(delete("/api/production/work_orders/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
    }

    @Test
    @DisplayName("暂停工单：合法请求转发原因给 Service 并返回 00000")
    void pauseWorkOrderForwardsReason() throws Exception {
        mockMvc.perform(put("/api/production/work_orders/100/pause")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "羽片缺料"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(workOrderService).pauseWorkOrder(100L, "羽片缺料");
    }

    @Test
    @DisplayName("暂停工单：原因为空白返回 A0400 且不调用 Service")
    void pauseWorkOrderRejectsBlankReason() throws Exception {
        mockMvc.perform(put("/api/production/work_orders/100/pause")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));

        verify(workOrderService, never()).pauseWorkOrder(any(), any());
    }

    @Test
    @DisplayName("恢复工单：转发 Service 并返回 00000")
    void resumeWorkOrderReturnsSuccess() throws Exception {
        mockMvc.perform(put("/api/production/work_orders/100/resume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(workOrderService).resumeWorkOrder(100L);
    }

    @Test
    @DisplayName("完工工单：Service 抛超限异常时返回 400 与 A0420")
    void finishWorkOrderReturnsBusinessError() throws Exception {
        doThrow(new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_FINISH_EXCEED_LIMIT))
                .when(workOrderService).finishWorkOrder(100L);

        mockMvc.perform(put("/api/production/work_orders/100/finish"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0420"))
                .andExpect(jsonPath("$.userTip").isNotEmpty());
    }

    @Test
    @DisplayName("关闭工单：转发 Service 并返回 00000")
    void closeWorkOrderReturnsSuccess() throws Exception {
        mockMvc.perform(put("/api/production/work_orders/100/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(workOrderService).closeWorkOrder(100L);
    }

    @Test
    @DisplayName("作废工单：缺少请求体返回 A0427 解析失败且不调用 Service")
    void cancelWorkOrderRejectsMissingBody() throws Exception {
        mockMvc.perform(put("/api/production/work_orders/100/cancel")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0427"));

        verify(workOrderService, never()).cancelWorkOrder(any(), any());
    }

    @Test
    @DisplayName("查询物料需求：返回列表数据")
    void getWorkOrderMaterialsReturnsList() throws Exception {
        when(workOrderService.getWorkOrderMaterials(100L)).thenReturn(List.of());

        mockMvc.perform(get("/api/production/work_orders/100/materials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("查询状态日志：操作时间按 yyyy-MM-dd HH:mm:ss 输出(API-013)")
    void getWorkOrderStatusLogsFormatsDateTime() throws Exception {
        WorkOrderStatusLogRespVO statusLog = new WorkOrderStatusLogRespVO();
        statusLog.setId(1L);
        statusLog.setWorkOrderId(100L);
        statusLog.setFromStatus(0);
        statusLog.setToStatus(1);
        statusLog.setChangeType(1);
        statusLog.setOperateTime(LocalDateTime.of(2026, 7, 10, 8, 0, 0));
        when(workOrderService.getWorkOrderStatusLogs(100L)).thenReturn(List.of(statusLog));

        mockMvc.perform(get("/api/production/work_orders/100/status_logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data[0].operateTime").value("2026-07-10 08:00:00"));
    }
}
