package com.badminton.mes.module.craft.controller;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.AuthInterceptor;
import com.badminton.mes.module.craft.controller.vo.CraftProcessSaveReqVO;
import com.badminton.mes.module.craft.service.CraftProcessDefectReasonService;
import com.badminton.mes.module.craft.service.CraftProcessService;
import com.badminton.mes.module.craft.service.CraftProcessSopService;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 工序管理 Controller Web 切片测试。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@WebMvcTest({CraftProcessController.class, CraftProcessSopController.class,
        CraftProcessDefectReasonController.class})
class CraftProcessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CraftProcessService processService;

    @MockitoBean
    private CraftProcessSopService sopService;

    @MockitoBean
    private CraftProcessDefectReasonService defectReasonService;

    @MockitoBean
    private AuthInterceptor authInterceptor;

    @BeforeEach
    void permitAllRequests() throws Exception {
        when(authInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("创建工序：合法请求返回新工序 id")
    void createProcessReturnsId() throws Exception {
        when(processService.createProcess(any(CraftProcessSaveReqVO.class))).thenReturn(100L);

        mockMvc.perform(post("/api/craft/processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProcessJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(100));
    }

    @Test
    @DisplayName("创建工序：非 ASCII 编码返回参数错误")
    void createProcessRejectsNonAsciiCode() throws Exception {
        mockMvc.perform(post("/api/craft/processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProcessJson().replace("FEATHER-FIX", "植毛-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
        verify(processService, never()).createProcess(any());
    }

    @Test
    @DisplayName("修改工序：缺少预期版本返回参数错误")
    void updateProcessRejectsMissingVersion() throws Exception {
        mockMvc.perform(put("/api/craft/processes/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProcessJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
        verify(processService, never()).updateProcess(any(), any());
    }

    @Test
    @DisplayName("状态变更：缺少预期版本返回参数错误")
    void updateStatusRejectsMissingVersion() throws Exception {
        mockMvc.perform(put("/api/craft/processes/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": 0, "reason": "工艺调整"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
        verify(processService, never()).updateProcessStatus(any(), any());
    }

    @Test
    @DisplayName("删除工序：预期版本作为必填查询参数转发")
    void deleteProcessForwardsExpectedVersion() throws Exception {
        mockMvc.perform(delete("/api/craft/processes/100").param("version", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));
        verify(processService).deleteProcess(100L, 3);
    }

    @Test
    @DisplayName("删除工序：缺少预期版本查询参数返回参数错误")
    void deleteProcessRejectsMissingVersion() throws Exception {
        mockMvc.perform(delete("/api/craft/processes/100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
        verify(processService, never()).deleteProcess(any(), any());
    }

    @Test
    @DisplayName("变更日志：返回分页结构而非无界数组")
    void getChangeLogsReturnsPageResult() throws Exception {
        when(processService.getProcessChangeLogPage(any(), any()))
                .thenReturn(PageResult.of(List.of(), 0L, 1, 10));

        mockMvc.perform(get("/api/craft/processes/100/change_logs")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("修改 SOP：缺少预期版本返回参数错误")
    void updateSopRejectsMissingVersion() throws Exception {
        mockMvc.perform(put("/api/craft/processes/100/sops/200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSopJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
        verify(sopService, never()).updateProcessSop(any(), any(), any());
    }

    @Test
    @DisplayName("删除 SOP：预期版本作为必填查询参数转发")
    void deleteSopForwardsExpectedVersion() throws Exception {
        mockMvc.perform(delete("/api/craft/processes/100/sops/200").param("version", "2"))
                .andExpect(status().isOk());
        verify(sopService).deleteProcessSop(100L, 200L, 2);
    }

    @Test
    @DisplayName("修改不良原因：携带版本的合法请求转发 Service")
    void updateDefectReasonForwardsVersionedRequest() throws Exception {
        mockMvc.perform(put("/api/craft/processes/100/defect_reasons/300")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reasonCode": "BROKEN-FEATHER",
                                  "reasonName": "断羽",
                                  "status": 1,
                                  "version": 4
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));
        verify(defectReasonService).updateProcessDefectReason(any(), any(), any());
    }

    private String validProcessJson() {
        return """
                {
                  "processCode": "FEATHER-FIX",
                  "processName": "植毛",
                  "processType": "PROCESSING",
                  "standardTimeSeconds": 120,
                  "keyProcess": true,
                  "qualityRequired": false,
                  "scanRequired": true,
                  "pieceRateEnabled": true
                }
                """;
    }

    private String validSopJson() {
        return """
                {
                  "sopCode": "SOP-01",
                  "sopName": "植毛作业指导书",
                  "sopVersion": "V1.0",
                  "fileUrl": "/files/sop-01.pdf",
                  "status": 1
                }
                """;
    }
}
