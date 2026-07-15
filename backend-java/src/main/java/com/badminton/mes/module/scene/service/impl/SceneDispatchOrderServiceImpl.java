package com.badminton.mes.module.scene.service.impl;

import java.util.*;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.scene.constants.SceneErrorCodeConstants;
import com.badminton.mes.module.scene.controller.vo.*;
import com.badminton.mes.module.scene.dal.entity.*;
import com.badminton.mes.module.scene.dal.redis.SceneNumberSequence;
import com.badminton.mes.module.scene.dal.repository.*;
import com.badminton.mes.module.scene.dal.repository.SceneDependencyQueryRepository.RoutingOperationSnapshot;
import com.badminton.mes.module.scene.enums.*;
import com.badminton.mes.module.scene.service.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import jakarta.persistence.criteria.Predicate;

/** 工序派工 Service。 @author 刘涵 */
@Service
public class SceneDispatchOrderServiceImpl implements SceneDispatchOrderService {
    private final SceneDispatchOrderRepository orderRepository;private final SceneDispatchDetailRepository detailRepository;
    private final SceneProductionTaskRepository taskRepository;private final SceneDependencyQueryRepository dependencyRepository;
    private final SceneNumberSequence sequence;private final SceneDataScopeService dataScope;
    public SceneDispatchOrderServiceImpl(SceneDispatchOrderRepository orderRepository,SceneDispatchDetailRepository detailRepository,
            SceneProductionTaskRepository taskRepository,SceneDependencyQueryRepository dependencyRepository,
            SceneNumberSequence sequence,SceneDataScopeService dataScope){this.orderRepository=orderRepository;this.detailRepository=detailRepository;
        this.taskRepository=taskRepository;this.dependencyRepository=dependencyRepository;this.sequence=sequence;this.dataScope=dataScope;}
    @Override @Transactional(rollbackFor=Exception.class)
    public Long generate(SceneDispatchGenerateReqVO req){SceneProductionTaskEntity task=requireTask(req.getTaskId());dataScope.check(task.getWorkshopId(),task.getLineId());
        if(!Set.of(SceneTaskStatusEnum.AUDITED.getStatus(),SceneTaskStatusEnum.RELEASED.getStatus()).contains(task.getTaskStatus()))throw new ServiceException(SceneErrorCodeConstants.TASK_STATUS_INVALID);
        if(orderRepository.findFirstByTaskIdAndDispatchStatusInAndDeletedFalse(task.getId(),List.of(0,1,2,3)).isPresent())throw new ServiceException(SceneErrorCodeConstants.DISPATCH_ALREADY_EXISTS);
        List<RoutingOperationSnapshot> snapshots=dependencyRepository.findRoutingOperations(task.getRoutingId());
        if(snapshots.isEmpty()||snapshots.getFirst().sequence()!=1||snapshots.getLast().sequence()!=snapshots.size())throw new ServiceException(SceneErrorCodeConstants.ROUTING_NOT_AVAILABLE);
        SceneDispatchOrderEntity order=new SceneDispatchOrderEntity();order.setDispatchNo(sequence.nextDispatchNo());order.setTaskId(task.getId());order.setRoutingId(task.getRoutingId());order.setRoutingCode(task.getRoutingCode());order.setRoutingVersion(task.getRoutingVersion());order.setDispatchStatus(SceneDispatchStatusEnum.PENDING_CONFIRM.getStatus());order.setCreateBy(SecurityContextHolder.getRequiredLoginUserId());orderRepository.saveAndFlush(order);
        List<SceneDispatchDetailEntity> details=snapshots.stream().map(s->{SceneDispatchDetailEntity d=new SceneDispatchDetailEntity();d.setDispatchId(order.getId());d.setTaskId(task.getId());d.setProcessId(s.processId());d.setProcessCode(s.processCode());d.setProcessName(s.processName());d.setSeq(s.sequence());d.setKeyProcess(s.keyProcess());d.setInspect(s.inspect());d.setScanRequired(s.scanRequired());d.setSopId(s.sopId());d.setSopCode(s.sopCode());d.setSopName(s.sopName());d.setSopVersion(s.sopVersion());d.setStationId(s.stationId());d.setPlanQuantity(task.getPlanQuantity());d.setDetailStatus(SceneOperationStatusEnum.PENDING.getStatus());d.setPaused(false);return d;}).toList();
        detailRepository.saveAll(details);return order.getId();}
    @Override @Transactional(rollbackFor=Exception.class) public void confirm(Long id){SceneDispatchOrderEntity o=require(id);checkTask(o.getTaskId());if(orderRepository.transition(id,0,1)==0)invalid();}
    @Override @Transactional(rollbackFor=Exception.class) public void cancel(Long id){SceneDispatchOrderEntity o=require(id);checkTask(o.getTaskId());if(orderRepository.transition(id,0,4)==0)invalid();}
    @Override @Transactional(readOnly=true) public SceneDispatchOrderRespVO get(Long id){SceneDispatchOrderEntity o=require(id);checkTask(o.getTaskId());return toResp(o,true);}
    @Override @Transactional(readOnly=true) public List<SceneDispatchDetailRespVO> operations(Long id){SceneDispatchOrderEntity o=require(id);checkTask(o.getTaskId());return detailRepository.findByDispatchIdAndDeletedFalseOrderBySeqAsc(id).stream().map(this::toDetail).toList();}
    @Override @Transactional(readOnly=true) public PageResult<SceneDispatchOrderRespVO> page(SceneDispatchPageReqVO req){Specification<SceneDispatchOrderEntity> spec=(root,q,cb)->{List<Predicate> p=new ArrayList<>();p.add(cb.isFalse(root.get("deleted")));if(StringUtils.hasText(req.getDispatchNo()))p.add(cb.like(root.get("dispatchNo"),req.getDispatchNo()+"%"));if(req.getTaskId()!=null)p.add(cb.equal(root.get("taskId"),req.getTaskId()));if(req.getDispatchStatus()!=null)p.add(cb.equal(root.get("dispatchStatus"),req.getDispatchStatus()));return cb.and(p.toArray(new Predicate[0]));};long total=orderRepository.count(spec);if(total==0)return PageResult.empty(req.getPageNo(),req.getPageSize());int pages=(int)((total+req.getPageSize()-1)/req.getPageSize());int no=Math.min(req.getPageNo(),pages);Page<SceneDispatchOrderEntity> page=orderRepository.findAll(spec,PageRequest.of(no-1,req.getPageSize(),Sort.by(Sort.Direction.DESC,"id")));List<SceneDispatchOrderRespVO> list=page.getContent().stream().filter(o->{try{checkTask(o.getTaskId());return true;}catch(ServiceException e){return false;}}).map(o->toResp(o,false)).toList();return PageResult.of(list,total,no,req.getPageSize());}
    private SceneProductionTaskEntity requireTask(Long id){return taskRepository.findByIdAndDeletedFalse(id).orElseThrow(()->new ServiceException(SceneErrorCodeConstants.TASK_NOT_EXISTS));}
    private void checkTask(Long id){SceneProductionTaskEntity t=requireTask(id);dataScope.check(t.getWorkshopId(),t.getLineId());}
    private SceneDispatchOrderEntity require(Long id){return orderRepository.findByIdAndDeletedFalse(id).orElseThrow(()->new ServiceException(SceneErrorCodeConstants.DISPATCH_NOT_EXISTS));}
    private void invalid(){throw new ServiceException(SceneErrorCodeConstants.DISPATCH_STATUS_INVALID);}
    private SceneDispatchOrderRespVO toResp(SceneDispatchOrderEntity o,boolean details){SceneDispatchOrderRespVO v=new SceneDispatchOrderRespVO();v.setId(o.getId());v.setDispatchNo(o.getDispatchNo());v.setTaskId(o.getTaskId());v.setRoutingId(o.getRoutingId());v.setRoutingCode(o.getRoutingCode());v.setRoutingVersion(o.getRoutingVersion());v.setDispatchStatus(o.getDispatchStatus());v.setOperations(details?detailRepository.findByDispatchIdAndDeletedFalseOrderBySeqAsc(o.getId()).stream().map(this::toDetail).toList():List.of());return v;}
    private SceneDispatchDetailRespVO toDetail(SceneDispatchDetailEntity d){SceneDispatchDetailRespVO v=new SceneDispatchDetailRespVO();v.setId(d.getId());v.setProcessId(d.getProcessId());v.setProcessCode(d.getProcessCode());v.setProcessName(d.getProcessName());v.setSeq(d.getSeq());v.setKeyProcess(d.getKeyProcess());v.setInspect(d.getInspect());v.setScanRequired(d.getScanRequired());v.setSopId(d.getSopId());v.setSopCode(d.getSopCode());v.setSopName(d.getSopName());v.setSopVersion(d.getSopVersion());v.setStationId(d.getStationId());v.setUserId(d.getUserId());v.setEquipmentId(d.getEquipmentId());v.setPlanQuantity(d.getPlanQuantity());v.setDetailStatus(d.getDetailStatus());v.setPaused(d.getPaused());v.setPauseReason(d.getPauseReason());v.setActualStartTime(d.getActualStartTime());v.setActualEndTime(d.getActualEndTime());return v;}
}
