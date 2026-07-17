package com.badminton.mes.module.scene.service.impl;
import java.util.*;import com.badminton.mes.common.security.*;import com.badminton.mes.module.scene.dal.entity.SceneProductionTaskEntity;
import com.badminton.mes.module.scene.dal.redis.SceneNumberSequence;import com.badminton.mes.module.scene.dal.repository.*;
import com.badminton.mes.module.scene.enums.SceneTaskStatusEnum;import com.badminton.mes.module.scene.service.SceneDataScopeService;
import org.junit.jupiter.api.*;import org.mockito.*;import static org.mockito.Mockito.*;
/** 生产任务状态机聚焦测试。 @author 刘涵 */
class SceneProductionTaskServiceImplTest {
 @Mock SceneProductionTaskRepository taskRepository;@Mock SceneTaskOperateLogRepository logRepository;
 @Mock SceneDispatchDetailRepository detailRepository;@Mock SceneDependencyQueryRepository dependencyRepository;
 @Mock SceneNumberSequence sequence;@Mock SceneDataScopeService dataScope;SceneProductionTaskServiceImpl service;
 @BeforeEach void setUp(){MockitoAnnotations.openMocks(this);service=new SceneProductionTaskServiceImpl(taskRepository,logRepository,detailRepository,dependencyRepository,sequence,dataScope);LoginUser u=new LoginUser();u.setUserId(1L);u.setRoleCodes(List.of("ADMIN"));SecurityContextHolder.set("t",u);}
 @AfterEach void tearDown(){SecurityContextHolder.clear();}
 @Test void auditsPendingTaskWithCasAndLog(){SceneProductionTaskEntity t=new SceneProductionTaskEntity();t.setId(1L);t.setWorkshopId(2L);t.setLineId(3L);t.setTaskStatus(0);when(taskRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(t));when(taskRepository.transition(1L,0,1,null)).thenReturn(1);service.auditTask(1L);verify(logRepository).save(any());}
}
