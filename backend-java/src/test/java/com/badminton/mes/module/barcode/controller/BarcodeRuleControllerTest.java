package com.badminton.mes.module.barcode.controller;

import java.lang.reflect.Method;
import java.util.List;

import com.badminton.mes.common.security.AuthInterceptor;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRulePreviewRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleSaveReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleValidateRespVO;
import com.badminton.mes.module.barcode.service.BarcodeRuleService;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link BarcodeRuleController} Web 切片测试。
 *
 * <p>Service 层 Mock，仅验证 Web 层契约：嵌套明细参数校验、统一响应、
 * 预览/校验转发与敏感动作角色注解。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@WebMvcTest(BarcodeRuleController.class)
class BarcodeRuleControllerTest {

    /** 合法创建请求体：产品编码 + 日期 + 流水号 */
    private static final String VALID_SAVE_BODY = """
            {
              "ruleCode": "RULE01",
              "ruleName": "批次码规则",
              "barcodeTypeId": 100,
              "serialLength": 4,
              "serialResetCycle": 1,
              "items": [
                {"seq": 1, "itemType": 3, "itemValue": "productCode"},
                {"seq": 2, "itemType": 2, "dateFormat": "yyyyMMdd"},
                {"seq": 3, "itemType": 4}
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BarcodeRuleService barcodeRuleService;

    /** Mock 掉登录鉴权拦截器：Web 切片无 Redis 会话依赖，鉴权逻辑由拦截器自身测试覆盖 */
    @MockitoBean
    private AuthInterceptor authInterceptor;

    @BeforeEach
    void permitAllRequests() throws Exception {
        when(authInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("新增规则：合法请求返回 00000 与新规则 id")
    void createBarcodeRuleReturnsId() throws Exception {
        when(barcodeRuleService.createBarcodeRule(any(BarcodeRuleSaveReqVO.class))).thenReturn(200L);

        mockMvc.perform(post("/api/barcode/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_SAVE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(200));
    }

    @Test
    @DisplayName("新增规则：组成明细为空返回 A0400 且不调用 Service")
    void createBarcodeRuleRejectsEmptyItems() throws Exception {
        mockMvc.perform(post("/api/barcode/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleCode": "RULE01",
                                  "ruleName": "批次码规则",
                                  "barcodeTypeId": 100,
                                  "serialLength": 4,
                                  "serialResetCycle": 1,
                                  "items": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));

        verify(barcodeRuleService, never()).createBarcodeRule(any());
    }

    @Test
    @DisplayName("新增规则：嵌套明细缺组成类型返回 A0400(级联校验)")
    void createBarcodeRuleRejectsInvalidNestedItem() throws Exception {
        mockMvc.perform(post("/api/barcode/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleCode": "RULE01",
                                  "ruleName": "批次码规则",
                                  "barcodeTypeId": 100,
                                  "serialLength": 4,
                                  "serialResetCycle": 1,
                                  "items": [
                                    {"seq": 1}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("itemType")));
    }

    @Test
    @DisplayName("新增规则：流水位数超上限 9 返回 A0400")
    void createBarcodeRuleRejectsOversizedSerialLength() throws Exception {
        mockMvc.perform(post("/api/barcode/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleCode": "RULE01",
                                  "ruleName": "批次码规则",
                                  "barcodeTypeId": 100,
                                  "serialLength": 10,
                                  "serialResetCycle": 1,
                                  "items": [
                                    {"seq": 1, "itemType": 4}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
    }

    @Test
    @DisplayName("预览规则：返回试算条码与分段结果")
    void previewBarcodeRuleReturnsComposedValue() throws Exception {
        BarcodeRulePreviewRespVO respVO = new BarcodeRulePreviewRespVO();
        respVO.setBarcodeValue("YMQ01202607120001");
        respVO.setTotalLength(17);
        respVO.setSerialCapacity(9999L);
        when(barcodeRuleService.previewBarcodeRule(any())).thenReturn(respVO);

        mockMvc.perform(post("/api/barcode/rules/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serialLength": 4,
                                  "sampleProductCode": "YMQ01",
                                  "items": [
                                    {"seq": 1, "itemType": 3, "itemValue": "productCode"},
                                    {"seq": 2, "itemType": 2, "dateFormat": "yyyyMMdd"},
                                    {"seq": 3, "itemType": 4}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.barcodeValue").value("YMQ01202607120001"))
                .andExpect(jsonPath("$.data.serialCapacity").value(9999));
    }

    @Test
    @DisplayName("校验规则：返回逐条错误说明")
    void validateBarcodeRuleReturnsErrors() throws Exception {
        BarcodeRuleValidateRespVO respVO = new BarcodeRuleValidateRespVO();
        respVO.setValid(false);
        respVO.setErrors(List.of("规则必须且只能包含一个流水号组成项"));
        when(barcodeRuleService.validateBarcodeRule(any())).thenReturn(respVO);

        mockMvc.perform(post("/api/barcode/rules/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serialLength": 4,
                                  "items": [
                                    {"seq": 1, "itemType": 1, "itemValue": "BM"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.valid").value(false))
                .andExpect(jsonPath("$.data.errors[0]")
                        .value(org.hamcrest.Matchers.containsString("流水号")));
    }

    @Test
    @DisplayName("分页查询：pageSize 超过上限 100 返回 A0400(FLOW-012 入参保护)")
    void getBarcodeRulePageRejectsOversizedPageSize() throws Exception {
        mockMvc.perform(get("/api/barcode/rules/page")
                        .param("pageNo", "1")
                        .param("pageSize", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
    }

    @Test
    @DisplayName("删除规则：路径参数为非正数返回 A0400(方法级校验)")
    void deleteBarcodeRuleRejectsNonPositiveId() throws Exception {
        mockMvc.perform(delete("/api/barcode/rules/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));

        verify(barcodeRuleService, never()).deleteBarcodeRule(any());
    }

    @Test
    @DisplayName("角色契约：配置类写动作限管理员与 PMC，预览/校验/查询登录即可")
    void writeActionsRequireAdminOrPmcRoles() {
        List<String> writeActions = List.of("createBarcodeRule", "updateBarcodeRule",
                "enableBarcodeRule", "disableBarcodeRule", "deleteBarcodeRule");
        List<String> readActions = List.of("previewBarcodeRule", "validateBarcodeRule",
                "getBarcodeRule", "getBarcodeRulePage");

        for (Method method : BarcodeRuleController.class.getDeclaredMethods()) {
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
