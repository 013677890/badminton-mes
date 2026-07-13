package com.badminton.mes.module.barcode.controller;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import com.badminton.mes.common.security.AuthInterceptor;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplatePreviewRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplateSaveReqVO;
import com.badminton.mes.module.barcode.service.BarcodeTemplateService;

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
 * {@link BarcodeTemplateController} Web 切片测试。
 *
 * <p>Service 层 Mock，仅验证 Web 层契约：嵌套字段参数校验、统一响应、
 * 预览转发与敏感动作角色注解。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@WebMvcTest(BarcodeTemplateController.class)
class BarcodeTemplateControllerTest {

    /** 合法创建请求体：条码字段 + 文本字段 */
    private static final String VALID_SAVE_BODY = """
            {
              "templateCode": "TPL01",
              "templateName": "产品标签",
              "paperWidth": 60.00,
              "paperHeight": 40.00,
              "fields": [
                {"fieldName": "条码值", "fieldType": 2, "dataSource": "barcodeValue",
                 "posX": 1.00, "posY": 1.00},
                {"fieldName": "产品名称", "fieldType": 1, "dataSource": "productName",
                 "posX": 1.00, "posY": 10.00, "fontSize": 12}
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BarcodeTemplateService barcodeTemplateService;

    /** Mock 掉登录鉴权拦截器：Web 切片无 Redis 会话依赖，鉴权逻辑由拦截器自身测试覆盖 */
    @MockitoBean
    private AuthInterceptor authInterceptor;

    @BeforeEach
    void permitAllRequests() throws Exception {
        when(authInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("新增模板：合法请求返回 00000 与新模板 id")
    void createBarcodeTemplateReturnsId() throws Exception {
        when(barcodeTemplateService.createBarcodeTemplate(any(BarcodeTemplateSaveReqVO.class)))
                .thenReturn(300L);

        mockMvc.perform(post("/api/barcode/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_SAVE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(300));
    }

    @Test
    @DisplayName("新增模板：字段列表为空返回 A0400 且不调用 Service")
    void createBarcodeTemplateRejectsEmptyFields() throws Exception {
        mockMvc.perform(post("/api/barcode/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode": "TPL01",
                                  "templateName": "产品标签",
                                  "paperWidth": 60.00,
                                  "paperHeight": 40.00,
                                  "fields": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));

        verify(barcodeTemplateService, never()).createBarcodeTemplate(any());
    }

    @Test
    @DisplayName("新增模板：嵌套字段缺数据来源返回 A0400(级联校验)")
    void createBarcodeTemplateRejectsInvalidNestedField() throws Exception {
        mockMvc.perform(post("/api/barcode/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode": "TPL01",
                                  "templateName": "产品标签",
                                  "paperWidth": 60.00,
                                  "paperHeight": 40.00,
                                  "fields": [
                                    {"fieldName": "条码值", "fieldType": 2,
                                     "posX": 1.00, "posY": 1.00}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("dataSource")));
    }

    @Test
    @DisplayName("新增模板：纸张宽度为 0 返回 A0400")
    void createBarcodeTemplateRejectsZeroPaperWidth() throws Exception {
        mockMvc.perform(post("/api/barcode/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode": "TPL01",
                                  "templateName": "产品标签",
                                  "paperWidth": 0,
                                  "paperHeight": 40.00,
                                  "fields": [
                                    {"fieldName": "条码值", "fieldType": 2, "dataSource": "barcodeValue",
                                     "posX": 1.00, "posY": 1.00}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
    }

    @Test
    @DisplayName("修改模板：合法请求转发 Service 并返回 00000")
    void updateBarcodeTemplateForwardsToService() throws Exception {
        mockMvc.perform(put("/api/barcode/templates/300")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_SAVE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(barcodeTemplateService).updateBarcodeTemplate(any(), any(BarcodeTemplateSaveReqVO.class));
    }

    @Test
    @DisplayName("预览模板：返回布局与逐字段展示内容")
    void previewBarcodeTemplateReturnsLayout() throws Exception {
        BarcodeTemplatePreviewRespVO respVO = new BarcodeTemplatePreviewRespVO();
        respVO.setTemplateId(300L);
        respVO.setTemplateCode("TPL01");
        respVO.setVersion("V1");
        respVO.setPaperWidth(new BigDecimal("60.00"));
        respVO.setPaperHeight(new BigDecimal("40.00"));
        when(barcodeTemplateService.previewBarcodeTemplate(any())).thenReturn(respVO);

        mockMvc.perform(post("/api/barcode/templates/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateId": 300,
                                  "sampleBarcodeValue": "YMQ01202607120001",
                                  "sampleData": {"productName": "羽毛球A级"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.templateCode").value("TPL01"))
                .andExpect(jsonPath("$.data.version").value("V1"));
    }

    @Test
    @DisplayName("预览模板：缺少模板 id 返回 A0400")
    void previewBarcodeTemplateRejectsMissingTemplateId() throws Exception {
        mockMvc.perform(post("/api/barcode/templates/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));

        verify(barcodeTemplateService, never()).previewBarcodeTemplate(any());
    }

    @Test
    @DisplayName("分页查询：pageSize 超过上限 100 返回 A0400(FLOW-012 入参保护)")
    void getBarcodeTemplatePageRejectsOversizedPageSize() throws Exception {
        mockMvc.perform(get("/api/barcode/templates/page")
                        .param("pageNo", "1")
                        .param("pageSize", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
    }

    @Test
    @DisplayName("角色契约：配置类写动作限管理员与 PMC，预览/查询登录即可")
    void writeActionsRequireAdminOrPmcRoles() {
        List<String> writeActions = List.of("createBarcodeTemplate", "updateBarcodeTemplate",
                "enableBarcodeTemplate", "disableBarcodeTemplate");
        List<String> readActions = List.of("previewBarcodeTemplate", "getBarcodeTemplate",
                "getBarcodeTemplatePage");

        for (Method method : BarcodeTemplateController.class.getDeclaredMethods()) {
            RequiresRoles requiresRoles = method.getAnnotation(RequiresRoles.class);
            if (writeActions.contains(method.getName())) {
                assertThat(requiresRoles).as("写动作 %s 必须声明角色限制", method.getName()).isNotNull();
                assertThat(requiresRoles.value())
                        .containsExactlyInAnyOrder(RoleCodeConstants.ADMIN, RoleCodeConstants.PMC);
            } else if (readActions.contains(method.getName())) {
                assertThat(requiresRoles).as("查询动作 %s 登录即可，不应限角色", method.getName()).isNull();
            }
        }
    }
}
