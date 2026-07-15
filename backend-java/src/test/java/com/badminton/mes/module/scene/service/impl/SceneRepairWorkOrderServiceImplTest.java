package com.badminton.mes.module.scene.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.*;
import com.badminton.mes.module.scene.controller.vo.SceneRepairCreateReqVO;
import com.badminton.mes.module.scene.dal.entity.*;
import com.badminton.mes.module.scene.dal.repository.*;
import com.badminton.mes.module.scene.service.SceneDataScopeService;
import java.util.*;
import org.junit.jupiter.api.*;

class SceneRepairWorkOrderServiceImplTest {
    private SceneRepairWorkOrderRepository workOrders;
    private SceneWorkReportRepository reports;
    private SceneProductionTaskRepository tasks;
    private SceneRepairWorkOrderServiceImpl service;

    @BeforeEach void setUp() {
        workOrders = mock(SceneRepairWorkOrderRepository.class); reports = mock(SceneWorkReportRepository.class);
        tasks = mock(SceneProductionTaskRepository.class);
        service = new SceneRepairWorkOrderServiceImpl(workOrders, mock(SceneRepairRecordRepository.class), reports, tasks,
                mock(SceneRepairRecheckRecordRepository.class), new SceneDataScopeService());
        LoginUser user = new LoginUser(); user.setUserId(1L); user.setRoleCodes(List.of(RoleCodeConstants.ADMIN));
        SecurityContextHolder.set("test", user);
    }
    @AfterEach void tearDown() { SecurityContextHolder.clear(); }

    @Test void shouldRejectQuantityAboveSourceDefect() {
        SceneWorkReportEntity report = new SceneWorkReportEntity(); report.setId(9L); report.setTaskId(2L);
        report.setBatchNo("B1"); report.setRecordType(1); report.setDefectQuantity(3);
        SceneProductionTaskEntity task = new SceneProductionTaskEntity(); task.setId(2L); task.setWorkshopId(1L); task.setLineId(1L);
        when(workOrders.findBySourceReportIdAndDeletedFalse(9L)).thenReturn(Optional.empty());
        when(reports.findByIdAndDeletedFalse(9L)).thenReturn(Optional.of(report)); when(tasks.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(task));
        SceneRepairCreateReqVO request = new SceneRepairCreateReqVO(); request.setSourceReportId(9L); request.setBatchNo("B1");
        request.setDefectQuantity(3); request.setRepairQuantity(4); request.setReason("返修");
        assertThrows(ServiceException.class, () -> service.create(request)); verify(workOrders, never()).save(any());
    }
}
