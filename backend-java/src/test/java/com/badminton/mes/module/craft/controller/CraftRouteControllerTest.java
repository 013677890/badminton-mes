package com.badminton.mes.module.craft.controller;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.AuthInterceptor;
import com.badminton.mes.module.craft.controller.vo.CraftRouteNewVersionReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteSaveReqVO;
import com.badminton.mes.module.craft.service.CraftRouteService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
 * 工艺路线 Controller Web 切片测试。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@WebMvcTest(CraftRouteController.class)
class CraftRouteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CraftRouteService routeService;

    @MockitoBean
    private AuthInterceptor authInterceptor;

    @BeforeEach
    void permitAllRequests() throws Exception {
        when(authInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("创建路线：合法聚合请求返回新路线 id")
    void createRouteReturnsId() throws Exception {
        when(routeService.createRoute(any(CraftRouteSaveReqVO.class))).thenReturn(100L);

        mockMvc.perform(post("/api/craft/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRouteJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(100));
    }

    @Test
    @DisplayName("创建路线：空步骤列表返回参数错误")
    void createRouteRejectsEmptySteps() throws Exception {
        mockMvc.perform(post("/api/craft/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRouteJson().replace(stepsJson(), "[]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
        verify(routeService, never()).createRoute(any());
    }

    @Test
    @DisplayName("创建路线：步骤数组包含 null 时返回参数错误而不是服务端异常")
    void createRouteRejectsNullStep() throws Exception {
        mockMvc.perform(post("/api/craft/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRouteJson().replace(stepsJson(), "[null]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
        verify(routeService, never()).createRoute(any());
    }

    @Test
    @DisplayName("修改路线：缺少预期版本返回参数错误")
    void updateRouteRejectsMissingVersion() throws Exception {
        mockMvc.perform(put("/api/craft/routes/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRouteJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
        verify(routeService, never()).updateRoute(any(), any());
    }

    @Test
    @DisplayName("审核路线：空白原因返回参数错误")
    void approveRouteRejectsBlankReason() throws Exception {
        mockMvc.perform(put("/api/craft/routes/100/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version": 0, "reason": "  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
        verify(routeService, never()).approveRoute(any(), any());
    }

    @Test
    @DisplayName("停用路线：携带版本与原因的请求转发 Service")
    void disableRouteForwardsVersionedRequest() throws Exception {
        mockMvc.perform(put("/api/craft/routes/100/disable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version": 2, "reason": "工艺淘汰"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));
        verify(routeService).disableRoute(eq(100L), any());
    }

    @Test
    @DisplayName("创建新版本：合法请求返回新版本路线 id")
    void createRouteVersionReturnsNewId() throws Exception {
        when(routeService.createRouteVersion(eq(100L), any(CraftRouteNewVersionReqVO.class)))
                .thenReturn(200L);

        mockMvc.perform(post("/api/craft/routes/100/versions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version": 3, "newRoutingVersion": "V2.0", "reason": "年度工艺升级"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(200));
    }

    @Test
    @DisplayName("删除路线：预期版本作为必填查询参数转发")
    void deleteRouteForwardsExpectedVersion() throws Exception {
        mockMvc.perform(delete("/api/craft/routes/100").param("version", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));
        verify(routeService).deleteRoute(100L, 3);
    }

    @Test
    @DisplayName("默认路线查询：缺少产品参数返回参数错误")
    void getDefaultRouteRejectsMissingProductId() throws Exception {
        mockMvc.perform(get("/api/craft/routes/default"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
        verify(routeService, never()).getDefaultRoute(any());
    }

    @Test
    @DisplayName("变更日志：返回分页结构而非无界数组")
    void getChangeLogsReturnsPageResult() throws Exception {
        when(routeService.getRouteChangeLogPage(any(), any()))
                .thenReturn(PageResult.of(List.of(), 0L, 1, 10));

        mockMvc.perform(get("/api/craft/routes/100/change_logs")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    private String validRouteJson() {
        return """
                {
                  "routingCode": "RT-SHUTTLE",
                  "routingName": "羽毛球标准工艺",
                  "routingVersion": "V1.0",
                  "sourceType": 1,
                  "productIds": [1],
                  "steps": %s
                }
                """.formatted(stepsJson());
    }

    private String stepsJson() {
        return """
                [{"sequenceNo": 1, "processId": 10, "inspectNode": false}]""";
    }
}
