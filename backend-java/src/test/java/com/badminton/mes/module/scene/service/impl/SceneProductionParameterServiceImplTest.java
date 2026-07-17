package com.badminton.mes.module.scene.service.impl;

import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.scene.controller.vo.SceneProductionParameterSaveReqVO;
import com.badminton.mes.module.scene.dal.repository.SceneParameterChangeLogRepository;
import com.badminton.mes.module.scene.dal.repository.SceneProductionParameterRepository;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 生产参数 Service 聚焦测试。 @author 刘涵 */
class SceneProductionParameterServiceImplTest {
    @Mock private SceneProductionParameterRepository parameterRepository;
    @Mock private SceneParameterChangeLogRepository logRepository;
    private SceneProductionParameterServiceImpl service;

    @BeforeEach void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new SceneProductionParameterServiceImpl(parameterRepository, logRepository);
        LoginUser user = new LoginUser(); user.setUserId(9L); user.setRoleCodes(List.of("ADMIN"));
        SecurityContextHolder.set("token", user);
    }
    @AfterEach void tearDown() { SecurityContextHolder.clear(); }

    @Test void createsSwitchParameterAndWritesInitialLog() {
        when(parameterRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            var entity = invocation.getArgument(0, com.badminton.mes.module.scene.dal.entity.SceneProductionParameterEntity.class);
            entity.setId(1L); return entity;
        });
        SceneProductionParameterSaveReqVO request = new SceneProductionParameterSaveReqVO();
        request.setParamCode("must_scan_report"); request.setParamName("必须扫码");
        request.setParamValue("1"); request.setValueType(1); request.setChangeReason("初始化");
        assertThat(service.createParameter(request)).isEqualTo(1L);
        verify(logRepository).save(any());
    }
}
