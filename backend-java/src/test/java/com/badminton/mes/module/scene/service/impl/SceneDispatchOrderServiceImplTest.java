package com.badminton.mes.module.scene.service.impl;
import java.util.*;import com.badminton.mes.common.security.*;import com.badminton.mes.module.scene.controller.vo.SceneDispatchGenerateReqVO;
import com.badminton.mes.module.scene.dal.entity.*;import com.badminton.mes.module.scene.dal.redis.SceneNumberSequence;
import com.badminton.mes.module.scene.dal.repository.*;import com.badminton.mes.module.scene.dal.repository.SceneDependencyQueryRepository.RoutingOperationSnapshot;
import com.badminton.mes.module.scene.service.SceneDataScopeService;import org.junit.jupiter.api.*;import org.mockito.*;
import static org.assertj.core.api.Assertions.assertThat;import static org.mockito.ArgumentMatchers.any;import static org.mockito.Mockito.*;
/** 工序派工生成聚焦测试。 @author 刘涵 */
class SceneDispatchOrderServiceImplTest {
 @Mock SceneDispatchOrderRepository orderRepository;@Mock SceneDispatchDetailRepository detailRepository;@Mock SceneProductionTaskRepository taskRepository;
 @Mock SceneDependencyQueryRepository dependencyRepository;@Mock SceneNumberSequence sequence;@Mock SceneDataScopeService scope;SceneDispatchOrderServiceImpl service;
 @BeforeEach void setUp(){MockitoAnnotations.openMocks(this);service=new SceneDispatchOrderServiceImpl(orderRepository,detailRepository,taskRepository,dependencyRepository,sequence,scope);LoginUser u=new LoginUser();u.setUserId(1L);u.setRoleCodes(List.of("ADMIN"));SecurityContextHolder.set("t",u);}
 @AfterEach void tearDown(){SecurityContextHolder.clear();}
 @Test void generatesSnapshotsInRoutingOrder(){SceneProductionTaskEntity t=new SceneProductionTaskEntity();t.setId(1L);t.setTaskStatus(1);t.setRoutingId(2L);t.setRoutingCode("R");t.setRoutingVersion("V1");t.setPlanQuantity(10);t.setWorkshopId(3L);t.setLineId(4L);when(taskRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(t));when(orderRepository.findFirstByTaskIdAndDispatchStatusInAndDeletedFalse(eq(1L),any())).thenReturn(Optional.empty());when(sequence.nextDispatchNo()).thenReturn("PD1");when(orderRepository.saveAndFlush(any())).thenAnswer(i->{SceneDispatchOrderEntity o=i.getArgument(0);o.setId(9L);return o;});when(dependencyRepository.findRoutingOperations(2L)).thenReturn(List.of(new RoutingOperationSnapshot(5L,"P1","工序",1,true,false,true,null,null,null,null,null)));SceneDispatchGenerateReqVO r=new SceneDispatchGenerateReqVO();r.setTaskId(1L);assertThat(service.generate(r)).isEqualTo(9L);verify(detailRepository).saveAll(any());}
}
