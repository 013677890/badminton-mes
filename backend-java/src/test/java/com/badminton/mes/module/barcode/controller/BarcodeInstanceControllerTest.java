package com.badminton.mes.module.barcode.controller;

import java.lang.reflect.Method;
import java.util.List;

import com.badminton.mes.common.security.AuthInterceptor;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.barcode.controller.vo.BarcodeGenerateReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeGenerateRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeParseRespVO;
import com.badminton.mes.module.barcode.service.BarcodeInstanceService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
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
 * {@link BarcodeInstanceController} Web 切片测试。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@WebMvcTest(BarcodeInstanceController.class)
class BarcodeInstanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BarcodeInstanceService barcodeInstanceService;

    /** Mock 掉登录鉴权拦截器：Web 切片无 Redis 会话依赖，鉴权逻辑由拦截器自身测试覆盖 */
    @MockitoBean
    private AuthInterceptor authInterceptor;

    @BeforeEach
    void permitAllRequests() throws Exception {
        when(authInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("生成条码：合法请求返回 00000 与条码值")
    void generateBarcodeReturnsValue() throws Exception {
        BarcodeGenerateRespVO respVO = new BarcodeGenerateRespVO();
        respVO.setId(500L);
        respVO.setBarcodeValue("YMQ01202607120001");
        when(barcodeInstanceService.generateBarcode(any(BarcodeGenerateReqVO.class))).thenReturn(respVO);

        mockMvc.perform(post("/api/barcode/instances/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"applyRuleId": 400, "workOrderId": 600}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.barcodeValue").value("YMQ01202607120001"));
    }

    @Test
    @DisplayName("生成条码：缺少应用规则返回 A0400 且不调用 Service")
    void generateBarcodeRejectsMissingApplyRule() throws Exception {
        mockMvc.perform(post("/api/barcode/instances/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("applyRuleId")));

        verify(barcodeInstanceService, never()).generateBarcode(any());
    }

    @Test
    @DisplayName("批量生成：数量超过上限 500 返回 A0400")
    void batchGenerateRejectsOversizedQuantity() throws Exception {
        mockMvc.perform(post("/api/barcode/instances/batch_generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"applyRuleId": 400, "quantity": 501}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));

        verify(barcodeInstanceService, never()).batchGenerateBarcodes(any());
    }

    @Test
    @DisplayName("解析条码：合法请求返回业务上下文")
    void parseBarcodeReturnsContext() throws Exception {
        BarcodeParseRespVO respVO = new BarcodeParseRespVO();
        respVO.setId(500L);
        respVO.setBarcodeValue("YMQ01202607120001");
        respVO.setProductCode("YMQ01");
        when(barcodeInstanceService.parseBarcode(any())).thenReturn(respVO);

        mockMvc.perform(post("/api/barcode/instances/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"barcodeValue": "YMQ01202607120001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.productCode").value("YMQ01"));
    }

    @Test
    @DisplayName("解析条码：条码值为空白返回 A0400")
    void parseBarcodeRejectsBlankValue() throws Exception {
        mockMvc.perform(post("/api/barcode/instances/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"barcodeValue": "  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));

        verify(barcodeInstanceService, never()).parseBarcode(any());
    }

    @Test
    @DisplayName("作废条码：转发原因给 Service 并返回 00000")
    void cancelBarcodeForwardsReason() throws Exception {
        mockMvc.perform(put("/api/barcode/instances/500/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "打印错误"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(barcodeInstanceService).cancelBarcode(any(), any());
    }

    @Test
    @DisplayName("分页查询：pageSize 超过上限 100 返回 A0400(FLOW-012 入参保护)")
    void getBarcodeInstancePageRejectsOversizedPageSize() throws Exception {
        mockMvc.perform(get("/api/barcode/instances/page")
                        .param("pageNo", "1")
                        .param("pageSize", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
    }

    @Test
    @DisplayName("打印条码：返回打印记录事实与预览数据")
    void printBarcodeReturnsRecordAndPreview() throws Exception {
        com.badminton.mes.module.barcode.controller.vo.BarcodePrintRespVO respVO =
                new com.badminton.mes.module.barcode.controller.vo.BarcodePrintRespVO();
        respVO.setPrintRecordId(700L);
        respVO.setPrintCount(1);
        respVO.setTemplateVersion("V1");
        when(barcodeInstanceService.printBarcode(any(), any())).thenReturn(respVO);

        mockMvc.perform(post("/api/barcode/instances/500/print")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.printRecordId").value(700))
                .andExpect(jsonPath("$.data.printCount").value(1));
    }

    @Test
    @DisplayName("导入条码：返回成功数、失败数与逐条失败原因")
    void importBarcodesReturnsCounts() throws Exception {
        com.badminton.mes.module.barcode.controller.vo.BarcodeImportRespVO respVO =
                new com.badminton.mes.module.barcode.controller.vo.BarcodeImportRespVO();
        respVO.setTotalCount(2);
        respVO.setSuccessCount(1);
        respVO.setFailCount(1);
        respVO.setFailures(List.of());
        when(barcodeInstanceService.importBarcodes(any())).thenReturn(respVO);

        mockMvc.perform(post("/api/barcode/instances/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applyRuleId": 400,
                                  "items": [
                                    {"barcodeValue": "EXT-001"},
                                    {"barcodeValue": "EXT-002"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failCount").value(1));
    }

    @Test
    @DisplayName("导入条码：超过单次上限 500 条整单拒绝(M1 待确认事项②口径)")
    void importBarcodesRejectsOversizedBatch() throws Exception {
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < 501; i++) {
            if (i > 0) {
                items.append(',');
            }
            items.append("{\"barcodeValue\": \"EXT-").append(i).append("\"}");
        }

        mockMvc.perform(post("/api/barcode/instances/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applyRuleId\": 400, \"items\": [" + items + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));

        verify(barcodeInstanceService, never()).importBarcodes(any());
    }

    @Test
    @DisplayName("使用记录：返回列表数据")
    void getBarcodeUseRecordsReturnsList() throws Exception {
        when(barcodeInstanceService.getBarcodeUseRecords(500L)).thenReturn(List.of());

        mockMvc.perform(get("/api/barcode/instances/500/use_records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("角色契约：生成/作废/导入限管理员和 PMC，打印另放开班组长，解析/查询登录即可")
    void writeActionsRequireAdminOrPmcRoles() {
        List<String> writeActions = List.of("generateBarcode", "batchGenerateBarcodes",
                "cancelBarcode", "importBarcodes");
        List<String> readActions = List.of("parseBarcode", "getBarcodeInstance",
                "getBarcodeInstancePage", "getBarcodeUseRecords");

        for (Method method : BarcodeInstanceController.class.getDeclaredMethods()) {
            RequiresRoles requiresRoles = method.getAnnotation(RequiresRoles.class);
            if (writeActions.contains(method.getName())) {
                assertThat(requiresRoles).as("写动作 %s 必须声明角色限制", method.getName()).isNotNull();
                assertThat(requiresRoles.value())
                        .containsExactlyInAnyOrder(RoleCodeConstants.ADMIN, RoleCodeConstants.PMC);
            } else if ("printBarcode".equals(method.getName())) {
                assertThat(requiresRoles).as("打印动作必须声明角色限制").isNotNull();
                assertThat(requiresRoles.value()).containsExactlyInAnyOrder(RoleCodeConstants.ADMIN,
                        RoleCodeConstants.PMC, RoleCodeConstants.TEAM_LEADER);
            } else if (readActions.contains(method.getName())) {
                assertThat(requiresRoles).as("动作 %s 登录即可，不应限角色", method.getName()).isNull();
            }
        }
    }
}
