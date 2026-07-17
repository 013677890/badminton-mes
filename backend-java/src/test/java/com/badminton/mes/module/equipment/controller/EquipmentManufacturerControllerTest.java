package com.badminton.mes.module.equipment.controller;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.AuthInterceptor;
import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerSaveReqVO;
import com.badminton.mes.module.equipment.service.EquipmentManufacturerService;

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
 * {@link EquipmentManufacturerController} Web 切片测试。
 *
 * <p>通过 {@link WebMvcTest} 仅加载制造商控制器相关 MVC 组件，Service 和认证拦截器均以 Mockito
 * Bean 隔离。重点覆盖 JSON 字段校验、路径主键约束、统一成功/失败结构和分页边界，并验证校验失败
 * 不会产生任何业务层副作用。
 */
@WebMvcTest(EquipmentManufacturerController.class)
class EquipmentManufacturerControllerTest {

    /** 执行真实参数绑定、Bean Validation 和 JSON 序列化。 */
    @Autowired
    private MockMvc mockMvc;

    /** 制造商业务服务桩，隔离数据库和事务逻辑。 */
    @MockitoBean
    private EquipmentManufacturerService manufacturerService;

    /** 认证拦截器桩，由初始化方法统一放行。 */
    @MockitoBean
    private AuthInterceptor authInterceptor;

    /** 放行认证拦截，使测试只观察控制器输入输出契约。 */
    @BeforeEach
    void permitAllRequests() throws Exception {
        when(authInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("创建设备制造商：合法请求返回 00000 与新制造商 id")
    void createEquipmentManufacturerReturnsId() throws Exception {
        when(manufacturerService.createEquipmentManufacturer(any(EquipmentManufacturerSaveReqVO.class)))
                .thenReturn(200L);

        mockMvc.perform(post("/api/equipment/manufacturers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "manufacturerCode": "SUPPLIER",
                                  "manufacturerName": "设备供应商",
                                  "contactPhone": "13800138000",
                                  "contactEmail": "supplier@example.com",
                                  "status": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(200));
    }

    @Test
    @DisplayName("创建设备制造商：邮箱格式非法返回 A0400 且不调用 Service")
    void createEquipmentManufacturerRejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/equipment/manufacturers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "manufacturerCode": "SUPPLIER",
                                  "manufacturerName": "设备供应商",
                                  "contactEmail": "invalid-email",
                                  "status": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));

        verify(manufacturerService, never())
                .createEquipmentManufacturer(any(EquipmentManufacturerSaveReqVO.class));
    }

    @Test
    @DisplayName("修改设备制造商：合法请求转发路径主键并返回 00000")
    void updateEquipmentManufacturerForwardsPathId() throws Exception {
        mockMvc.perform(put("/api/equipment/manufacturers/200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "manufacturerCode": "SUPPLIER",
                                  "manufacturerName": "设备供应商",
                                  "contactPhone": "13800138000",
                                  "contactEmail": "supplier@example.com",
                                  "status": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(manufacturerService).updateEquipmentManufacturer(
                any(), any(EquipmentManufacturerSaveReqVO.class));
    }

    @Test
    @DisplayName("删除设备制造商：非正数路径参数返回 A0400")
    void deleteEquipmentManufacturerRejectsNonPositiveId() throws Exception {
        mockMvc.perform(delete("/api/equipment/manufacturers/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));

        verify(manufacturerService, never()).deleteEquipmentManufacturer(any());
    }

    @Test
    @DisplayName("查询设备制造商详情：返回 Service 响应数据")
    void getEquipmentManufacturerReturnsServiceResult() throws Exception {
        EquipmentManufacturerRespVO response = new EquipmentManufacturerRespVO();
        response.setId(200L);
        response.setManufacturerCode("SUPPLIER");
        response.setManufacturerName("设备供应商");
        response.setStatus(1);
        when(manufacturerService.getEquipmentManufacturer(200L)).thenReturn(response);

        mockMvc.perform(get("/api/equipment/manufacturers/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.manufacturerCode").value("SUPPLIER"));
    }

    @Test
    @DisplayName("设备制造商分页：pageSize 超过上限返回 A0400")
    void getEquipmentManufacturerPageRejectsOversizedPageSize() throws Exception {
        mockMvc.perform(get("/api/equipment/manufacturers/page")
                        .param("pageNo", "1")
                        .param("pageSize", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));

        verify(manufacturerService, never())
                .getEquipmentManufacturerPage(any(EquipmentManufacturerPageReqVO.class));
    }

    @Test
    @DisplayName("设备制造商分页：合法查询返回分页结构")
    void getEquipmentManufacturerPageReturnsPageResult() throws Exception {
        EquipmentManufacturerRespVO response = new EquipmentManufacturerRespVO();
        response.setId(200L);
        response.setManufacturerCode("SUPPLIER");
        response.setManufacturerName("设备供应商");
        when(manufacturerService.getEquipmentManufacturerPage(any(EquipmentManufacturerPageReqVO.class)))
                .thenReturn(PageResult.of(List.of(response), 1L, 1, 20));

        mockMvc.perform(get("/api/equipment/manufacturers/page")
                        .param("pageNo", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].manufacturerCode").value("SUPPLIER"));
    }
}
