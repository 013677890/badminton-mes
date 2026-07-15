package com.badminton.mes.module.barcode.controller;

import java.lang.reflect.Method;
import java.util.List;

import com.badminton.mes.common.security.AuthInterceptor;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.barcode.controller.vo.BarcodeApplicationRuleOptionReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeApplicationRuleSaveReqVO;
import com.badminton.mes.module.barcode.service.BarcodeApplicationRuleService;

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
 * {@link BarcodeApplicationRuleController} Web 切片测试。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@WebMvcTest(BarcodeApplicationRuleController.class)
class BarcodeApplicationRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BarcodeApplicationRuleService barcodeApplicationRuleService;

    /** Mock 掉登录鉴权拦截器：Web 切片无 Redis 会话依赖，鉴权逻辑由拦截器自身测试覆盖 */
    @MockitoBean
    private AuthInterceptor authInterceptor;

    @BeforeEach
    void permitAllRequests() throws Exception {
        when(authInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("新增应用规则：合法请求返回 00000 与新规则 id")
    void createApplicationRuleReturnsId() throws Exception {
        when(barcodeApplicationRuleService.createBarcodeApplicationRule(
                any(BarcodeApplicationRuleSaveReqVO.class))).thenReturn(400L);

        mockMvc.perform(post("/api/barcode/application_rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "objectType": 1,
                                  "productId": 10,
                                  "barcodeTypeId": 100,
                                  "barcodeMode": 2,
                                  "ruleId": 200,
                                  "templateId": 300,
                                  "sourceType": 1,
                                  "defaultFlag": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(400));
    }

    @Test
    @DisplayName("新增应用规则：缺少对象类型返回 A0400 且不调用 Service")
    void createApplicationRuleRejectsMissingObjectType() throws Exception {
        mockMvc.perform(post("/api/barcode/application_rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 10,
                                  "barcodeTypeId": 100,
                                  "barcodeMode": 2,
                                  "templateId": 300,
                                  "sourceType": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("objectType")));

        verify(barcodeApplicationRuleService, never()).createBarcodeApplicationRule(any());
    }

    @Test
    @DisplayName("选项查询：GET 参数绑定并返回列表数据")
    void getApplicationRuleOptionsReturnsList() throws Exception {
        when(barcodeApplicationRuleService.getBarcodeApplicationRuleOptions(
                any(BarcodeApplicationRuleOptionReqVO.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/barcode/application_rules/options")
                        .param("objectType", "1")
                        .param("productId", "10")
                        .param("barcodeTypeId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("选项查询：对象类型超出取值范围返回 A0400")
    void getApplicationRuleOptionsRejectsInvalidObjectType() throws Exception {
        mockMvc.perform(get("/api/barcode/application_rules/options")
                        .param("objectType", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
    }

    @Test
    @DisplayName("分页查询：pageSize 超过上限 100 返回 A0400(FLOW-012 入参保护)")
    void getApplicationRulePageRejectsOversizedPageSize() throws Exception {
        mockMvc.perform(get("/api/barcode/application_rules/page")
                        .param("pageNo", "1")
                        .param("pageSize", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
    }

    @Test
    @DisplayName("删除应用规则：路径参数为非正数返回 A0400(方法级校验)")
    void deleteApplicationRuleRejectsNonPositiveId() throws Exception {
        mockMvc.perform(delete("/api/barcode/application_rules/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));

        verify(barcodeApplicationRuleService, never()).deleteBarcodeApplicationRule(any());
    }

    @Test
    @DisplayName("角色契约：配置类写动作限管理员与 PMC，查询与选项登录即可")
    void writeActionsRequireAdminOrPmcRoles() {
        List<String> writeActions = List.of("createBarcodeApplicationRule",
                "updateBarcodeApplicationRule", "enableBarcodeApplicationRule",
                "disableBarcodeApplicationRule", "deleteBarcodeApplicationRule");
        List<String> readActions = List.of("getBarcodeApplicationRule",
                "getBarcodeApplicationRulePage", "getBarcodeApplicationRuleOptions");

        for (Method method : BarcodeApplicationRuleController.class.getDeclaredMethods()) {
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
