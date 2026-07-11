package com.badminton.mes.module.integration.controller;

import com.badminton.mes.common.security.AuthInterceptor;
import com.badminton.mes.module.integration.controller.vo.IntegrationWriteResultRespVO;
import com.badminton.mes.module.integration.service.IntegrationService;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link IntegrationController} Web 切片测试。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@WebMvcTest(IntegrationController.class)
class IntegrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IntegrationService integrationService;

    @MockitoBean
    private AuthInterceptor authInterceptor;

    @BeforeEach
    void permitAllRequests() throws Exception {
        when(authInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("单位写入：合法请求返回可追踪的写入结果")
    void writeUnitReturnsTraceableResult() throws Exception {
        IntegrationWriteResultRespVO result = new IntegrationWriteResultRespVO();
        result.setLogId(10L);
        result.setStatus("SUCCESS");
        result.setBusinessId(20L);
        result.setBusinessNo("PCS");
        when(integrationService.writeUnit(any())).thenReturn(result);

        mockMvc.perform(post("/api/integration/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceSystem": "ERP-MAIN",
                                  "unitCode": "PCS",
                                  "unitName": "个",
                                  "decimalPrecision": 0,
                                  "status": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.logId").value(10))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("外部工单写入：缺少来源工单号时返回参数错误")
    void writeWorkOrderRejectsMissingExternalNo() throws Exception {
        mockMvc.perform(post("/api/integration/work_orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validWorkOrderJson().replace(
                                "\"externalWorkOrderNo\": \"ERP-WO-001\",", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
        verify(integrationService, never()).writeWorkOrder(any());
    }

    @Test
    @DisplayName("外部工单写入：非法产品编码字符在 Web 层被拒绝")
    void writeWorkOrderRejectsInvalidProductCode() throws Exception {
        mockMvc.perform(post("/api/integration/work_orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validWorkOrderJson().replace("P001", "产品 001")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
        verify(integrationService, never()).writeWorkOrder(any());
    }

    @Test
    @DisplayName("日志查询：pageSize 超过上限时返回参数错误")
    void getWriteLogsRejectsOversizedPage() throws Exception {
        mockMvc.perform(get("/api/integration/write_logs")
                        .param("pageNo", "1")
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
        verify(integrationService, never()).getWriteLogPage(any());
    }

    private String validWorkOrderJson() {
        return """
                {
                  "sourceSystem": "ERP-MAIN",
                  "externalWorkOrderNo": "ERP-WO-001",
                  "productCode": "P001",
                  "workshopCode": "WS001",
                  "bomId": 30,
                  "routingId": 40,
                  "planQuantity": 1000,
                  "planStartTime": "2026-07-12 08:00:00",
                  "planEndTime": "2026-07-15 18:00:00"
                }
                """;
    }
}
