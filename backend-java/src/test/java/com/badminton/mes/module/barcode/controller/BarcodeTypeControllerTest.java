package com.badminton.mes.module.barcode.controller;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.AuthInterceptor;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.barcode.constants.BarcodeErrorCodeConstants;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTypeRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTypeSaveReqVO;
import com.badminton.mes.module.barcode.service.BarcodeTypeService;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link BarcodeTypeController} Web 切片测试。
 *
 * <p>Service 层 Mock，仅验证 Web 层契约：参数校验、统一响应结构、
 * HTTP 状态码映射与敏感动作角色注解(鉴权执行逻辑由拦截器自身测试覆盖)。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@WebMvcTest(BarcodeTypeController.class)
class BarcodeTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BarcodeTypeService barcodeTypeService;

    /** Mock 掉登录鉴权拦截器：Web 切片无 Redis 会话依赖，鉴权逻辑由拦截器自身测试覆盖 */
    @MockitoBean
    private AuthInterceptor authInterceptor;

    @BeforeEach
    void permitAllRequests() throws Exception {
        when(authInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("新增类型：合法请求返回 00000 与新类型 id")
    void createBarcodeTypeReturnsId() throws Exception {
        when(barcodeTypeService.createBarcodeType(any(BarcodeTypeSaveReqVO.class))).thenReturn(100L);

        mockMvc.perform(post("/api/barcode/types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "typeCode": "PRODUCT",
                                  "typeName": "产品码",
                                  "applyObject": "成品"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(100));
    }

    @Test
    @DisplayName("新增类型：编码为空白返回 A0400 且不调用 Service")
    void createBarcodeTypeRejectsBlankCode() throws Exception {
        mockMvc.perform(post("/api/barcode/types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "typeCode": "  ",
                                  "typeName": "产品码"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));

        verify(barcodeTypeService, never()).createBarcodeType(any());
    }

    @Test
    @DisplayName("新增类型：编码超长返回 A0400，message 指明字段")
    void createBarcodeTypeRejectsOversizedCode() throws Exception {
        mockMvc.perform(post("/api/barcode/types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "typeCode": "%s",
                                  "typeName": "产品码"
                                }
                                """.formatted("X".repeat(33))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("typeCode")));
    }

    @Test
    @DisplayName("修改类型：合法请求转发 Service 并返回 00000")
    void updateBarcodeTypeForwardsToService() throws Exception {
        mockMvc.perform(put("/api/barcode/types/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "typeCode": "PRODUCT",
                                  "typeName": "产品码"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(barcodeTypeService).updateBarcodeType(any(), any(BarcodeTypeSaveReqVO.class));
    }

    @Test
    @DisplayName("启用类型：转发 Service 并返回 00000")
    void enableBarcodeTypeReturnsSuccess() throws Exception {
        mockMvc.perform(put("/api/barcode/types/100/enable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(barcodeTypeService).enableBarcodeType(100L);
    }

    @Test
    @DisplayName("停用类型：转发 Service 并返回 00000")
    void disableBarcodeTypeReturnsSuccess() throws Exception {
        mockMvc.perform(put("/api/barcode/types/100/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(barcodeTypeService).disableBarcodeType(100L);
    }

    @Test
    @DisplayName("删除类型：路径参数为非正数返回 A0400(方法级校验)")
    void deleteBarcodeTypeRejectsNonPositiveId() throws Exception {
        mockMvc.perform(delete("/api/barcode/types/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));

        verify(barcodeTypeService, never()).deleteBarcodeType(any());
    }

    @Test
    @DisplayName("删除类型：被引用时返回 400、业务错误码与用户提示(API-003)")
    void deleteBarcodeTypeReturnsBusinessError() throws Exception {
        org.mockito.Mockito.doThrow(
                        new ServiceException(BarcodeErrorCodeConstants.BARCODE_TYPE_IN_USE_NOT_DELETE))
                .when(barcodeTypeService).deleteBarcodeType(100L);

        mockMvc.perform(delete("/api/barcode/types/100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0440"))
                .andExpect(jsonPath("$.userTip").isNotEmpty());
    }

    @Test
    @DisplayName("查询详情：时间字段按 yyyy-MM-dd HH:mm:ss 输出(API-013)")
    void getBarcodeTypeFormatsDateTime() throws Exception {
        BarcodeTypeRespVO respVO = new BarcodeTypeRespVO();
        respVO.setId(100L);
        respVO.setTypeCode("PRODUCT");
        respVO.setCreateTime(LocalDateTime.of(2026, 7, 12, 8, 0, 0));
        when(barcodeTypeService.getBarcodeType(100L)).thenReturn(respVO);

        mockMvc.perform(get("/api/barcode/types/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.typeCode").value("PRODUCT"))
                .andExpect(jsonPath("$.data.createTime").value("2026-07-12 08:00:00"));
    }

    @Test
    @DisplayName("分页查询：pageSize 超过上限 100 返回 A0400(FLOW-012 入参保护)")
    void getBarcodeTypePageRejectsOversizedPageSize() throws Exception {
        mockMvc.perform(get("/api/barcode/types/page")
                        .param("pageNo", "1")
                        .param("pageSize", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
    }

    @Test
    @DisplayName("启用选项：返回列表数据")
    void getEnabledBarcodeTypeOptionsReturnsList() throws Exception {
        when(barcodeTypeService.getEnabledBarcodeTypeOptions()).thenReturn(List.of());

        mockMvc.perform(get("/api/barcode/types/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("角色契约：配置类写动作限管理员与 PMC，查询动作登录即可")
    void writeActionsRequireAdminOrPmcRoles() throws Exception {
        List<String> writeActions = List.of("createBarcodeType", "updateBarcodeType",
                "enableBarcodeType", "disableBarcodeType", "deleteBarcodeType");
        List<String> readActions = List.of("getBarcodeType", "getBarcodeTypePage",
                "getEnabledBarcodeTypeOptions");

        for (Method method : BarcodeTypeController.class.getDeclaredMethods()) {
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
