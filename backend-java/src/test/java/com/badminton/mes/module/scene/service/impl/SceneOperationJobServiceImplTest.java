package com.badminton.mes.module.scene.service.impl;
import java.util.*;import com.badminton.mes.common.exception.ServiceException;import com.badminton.mes.common.security.*;
import com.badminton.mes.module.barcode.service.BarcodeSceneService;import com.badminton.mes.module.scene.dal.entity.*;
import com.badminton.mes.module.scene.dal.repository.*;import com.badminton.mes.module.scene.service.*;
import org.junit.jupiter.api.*;import org.mockito.*;import static org.assertj.core.api.Assertions.assertThatThrownBy;import static org.mockito.Mockito.*;
/** 工序作业扫码门禁聚焦测试。 @author 刘涵 */
class SceneOperationJobServiceImplTest {
 @Mock SceneDispatchDetailRepository detail;@Mock SceneDispatchOrderRepository order;@Mock SceneProductionTaskRepository task;
 @Mock SceneBatchStatusRepository batch;@Mock SceneBatchStatusHistoryRepository statusHistory;@Mock SceneBatchProcessHistoryRepository processHistory;
 @Mock BarcodeSceneService barcode;@Mock SceneProductionParameterService parameter;@Mock SceneDataScopeService scope;SceneOperationJobServiceImpl service;
 @BeforeEach void setUp(){MockitoAnnotations.openMocks(this);service=new SceneOperationJobServiceImpl(detail,order,task,batch,statusHistory,processHistory,barcode,parameter,scope);LoginUser u=new LoginUser();u.setUserId(1L);u.setRoleCodes(List.of("OPERATOR"));SecurityContextHolder.set("t",u);}
 @AfterEach void tearDown(){SecurityContextHolder.clear();}
 @Test void rejectsStartWhenRequiredScanMissing(){SceneDispatchDetailEntity d=new SceneDispatchDetailEntity();d.setId(1L);d.setTaskId(2L);d.setDetailStatus(0);d.setScanRequired(true);SceneProductionTaskEntity t=new SceneProductionTaskEntity();t.setId(2L);t.setWorkshopId(3L);t.setLineId(4L);when(detail.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(d));when(task.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(t));assertThatThrownBy(()->service.start(1L)).isInstanceOf(ServiceException.class);}
}
