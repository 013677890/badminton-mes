package com.badminton.mes.module.scene.service.impl;

import java.time.LocalDateTime;
import java.util.*;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.*;
import com.badminton.mes.module.scene.constants.SceneErrorCodeConstants;
import com.badminton.mes.module.scene.controller.vo.*;
import com.badminton.mes.module.scene.dal.entity.*;
import com.badminton.mes.module.scene.dal.redis.SceneNumberSequence;
import com.badminton.mes.module.scene.dal.repository.*;
import com.badminton.mes.module.scene.dal.repository.SceneDependencyQueryRepository.WorkOrderSnapshot;
import com.badminton.mes.module.scene.enums.*;
import com.badminton.mes.module.scene.service.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import jakarta.persistence.criteria.Predicate;

/** 生产任务 Service，状态流转使用数据库 CAS。 @author 刘涵 */
@Service
public class SceneProductionTaskServiceImpl implements SceneProductionTaskService {
    private final SceneProductionTaskRepository taskRepository;private final SceneTaskOperateLogRepository logRepository;
    private final SceneDispatchDetailRepository detailRepository;private final SceneDependencyQueryRepository dependencyRepository;
    private final SceneNumberSequence numberSequence;private final SceneDataScopeService dataScopeService;
    public SceneProductionTaskServiceImpl(SceneProductionTaskRepository taskRepository,SceneTaskOperateLogRepository logRepository,
            SceneDispatchDetailRepository detailRepository,SceneDependencyQueryRepository dependencyRepository,
            SceneNumberSequence numberSequence,SceneDataScopeService dataScopeService){
        this.taskRepository=taskRepository;this.logRepository=logRepository;this.detailRepository=detailRepository;
        this.dependencyRepository=dependencyRepository;this.numberSequence=numberSequence;this.dataScopeService=dataScopeService;
    }
    @Override @Transactional(rollbackFor=Exception.class)
    public Long createTask(SceneProductionTaskSaveReqVO req){
        validateTime(req);WorkOrderSnapshot snapshot=dependencyRepository.findReleasedWorkOrder(req.getWorkOrderId(),req.getLineId())
                .orElseThrow(()->new ServiceException(SceneErrorCodeConstants.WORK_ORDER_NOT_AVAILABLE));
        dataScopeService.check(snapshot.workshopId(),snapshot.lineId());
        long allocated=taskRepository.sumAllocated(snapshot.id(),List.of(SceneTaskStatusEnum.CANCELLED.getStatus(),SceneTaskStatusEnum.CLOSED.getStatus()));
        if(allocated+req.getPlanQuantity()>snapshot.planQuantity())throw new ServiceException(SceneErrorCodeConstants.TASK_QUANTITY_EXCEEDED);
        SceneProductionTaskEntity task=new SceneProductionTaskEntity();copyRequest(task,req);
        task.setTaskNo(numberSequence.nextTaskNo());task.setSourceType(2);task.setWorkOrderId(snapshot.id());task.setWorkOrderNo(snapshot.workOrderNo());
        task.setProductId(snapshot.productId());task.setProductCode(snapshot.productCode());task.setProductName(snapshot.productName());
        task.setBatchNo(snapshot.batchNo());task.setRoutingId(snapshot.routingId());task.setRoutingCode(snapshot.routingCode());task.setRoutingVersion(snapshot.routingVersion());
        task.setWorkshopId(snapshot.workshopId());task.setWorkshopName(snapshot.workshopName());task.setLineName(snapshot.lineName());
        task.setTaskStatus(SceneTaskStatusEnum.PENDING_AUDIT.getStatus());task.setCreateBy(SecurityContextHolder.getRequiredLoginUserId());
        taskRepository.saveAndFlush(task);return task.getId();
    }
    @Override @Transactional(rollbackFor=Exception.class)
    public void updateTask(Long id,SceneProductionTaskSaveReqVO req){validateTime(req);SceneProductionTaskEntity task=requireTask(id);
        if(!SceneTaskStatusEnum.PENDING_AUDIT.getStatus().equals(task.getTaskStatus()))invalid();
        WorkOrderSnapshot snapshot=dependencyRepository.findReleasedWorkOrder(req.getWorkOrderId(),req.getLineId()).orElseThrow(()->new ServiceException(SceneErrorCodeConstants.WORK_ORDER_NOT_AVAILABLE));
        dataScopeService.check(snapshot.workshopId(),snapshot.lineId());copyRequest(task,req);
        task.setWorkOrderNo(snapshot.workOrderNo());task.setProductId(snapshot.productId());
        task.setProductCode(snapshot.productCode());task.setProductName(snapshot.productName());
        task.setBatchNo(snapshot.batchNo());task.setRoutingId(snapshot.routingId());
        task.setRoutingCode(snapshot.routingCode());task.setRoutingVersion(snapshot.routingVersion());
        task.setWorkshopId(snapshot.workshopId());task.setWorkshopName(snapshot.workshopName());
        task.setLineName(snapshot.lineName());taskRepository.save(task);}
    @Override @Transactional(rollbackFor=Exception.class) public void auditTask(Long id){transition(id,SceneTaskStatusEnum.PENDING_AUDIT,SceneTaskStatusEnum.AUDITED,5,null);}
    @Override @Transactional(rollbackFor=Exception.class) public void releaseTask(Long id){transition(id,SceneTaskStatusEnum.AUDITED,SceneTaskStatusEnum.RELEASED,6,null);}
    @Override @Transactional(rollbackFor=Exception.class) public void startTask(Long id){transition(id,SceneTaskStatusEnum.RELEASED,SceneTaskStatusEnum.IN_PRODUCTION,1,null);}
    @Override @Transactional(rollbackFor=Exception.class) public void pauseTask(Long id,String reason){transition(id,SceneTaskStatusEnum.IN_PRODUCTION,SceneTaskStatusEnum.PAUSED,2,reason);}
    @Override @Transactional(rollbackFor=Exception.class) public void resumeTask(Long id){transition(id,SceneTaskStatusEnum.PAUSED,SceneTaskStatusEnum.IN_PRODUCTION,3,null);}
    @Override @Transactional(rollbackFor=Exception.class) public void closeTask(Long id,String reason){transition(id,SceneTaskStatusEnum.FINISHED,SceneTaskStatusEnum.CLOSED,7,reason);}
    private void transition(Long id,SceneTaskStatusEnum from,SceneTaskStatusEnum to,int operation,String reason){
        SceneProductionTaskEntity task=requireTask(id);dataScopeService.check(task.getWorkshopId(),task.getLineId());
        if(taskRepository.transition(id,from.getStatus(),to.getStatus(),reason)==0)invalid();
        if(to==SceneTaskStatusEnum.IN_PRODUCTION&&task.getActualStartTime()==null){task.setActualStartTime(LocalDateTime.now());taskRepository.save(task);}
        insertLog(id,operation,from.getStatus(),to.getStatus(),reason);
    }
    @Override @Transactional(readOnly=true) public SceneProductionTaskRespVO getTask(Long id){SceneProductionTaskEntity task=requireTask(id);dataScopeService.check(task.getWorkshopId(),task.getLineId());return toResp(task);}
    @Override @Transactional(readOnly=true) public PageResult<SceneProductionTaskRespVO> getTaskPage(SceneProductionTaskPageReqVO req){
        Specification<SceneProductionTaskEntity> spec=(root,q,cb)->{List<Predicate> p=new ArrayList<>();p.add(cb.isFalse(root.get("deleted")));
            LoginUser user=SecurityContextHolder.getRequiredLoginUser();boolean global=user.getRoleCodes().contains(RoleCodeConstants.ADMIN)||user.getRoleCodes().contains(RoleCodeConstants.PMC);
            if(!global&&user.getWorkshopId()!=null)p.add(cb.equal(root.get("workshopId"),user.getWorkshopId()));
            if(!global&&!user.getRoleCodes().contains(RoleCodeConstants.WORKSHOP_MANAGER)&&user.getLineId()!=null)p.add(cb.equal(root.get("lineId"),user.getLineId()));
            if(StringUtils.hasText(req.getTaskNo()))p.add(cb.like(root.get("taskNo"),req.getTaskNo()+"%"));if(req.getWorkshopId()!=null)p.add(cb.equal(root.get("workshopId"),req.getWorkshopId()));
            if(req.getLineId()!=null)p.add(cb.equal(root.get("lineId"),req.getLineId()));if(req.getTaskStatus()!=null)p.add(cb.equal(root.get("taskStatus"),req.getTaskStatus()));if(req.getPlanDate()!=null)p.add(cb.equal(root.get("planDate"),req.getPlanDate()));return cb.and(p.toArray(new Predicate[0]));};
        long total=taskRepository.count(spec);if(total==0)return PageResult.empty(req.getPageNo(),req.getPageSize());int pages=(int)((total+req.getPageSize()-1)/req.getPageSize());int no=Math.min(req.getPageNo(),pages);
        Page<SceneProductionTaskEntity> page=taskRepository.findAll(spec,PageRequest.of(no-1,req.getPageSize(),Sort.by(Sort.Direction.DESC,"id")));
        return PageResult.of(page.getContent().stream().map(this::toResp).toList(),total,no,req.getPageSize());}
    @Override @Transactional(readOnly=true) public SceneTaskProgressRespVO getTaskProgress(Long id){SceneProductionTaskEntity task=requireTask(id);dataScopeService.check(task.getWorkshopId(),task.getLineId());
        List<SceneDispatchDetailEntity> list=detailRepository.findByTaskIdAndDeletedFalseOrderBySeqAsc(id);SceneTaskProgressRespVO v=new SceneTaskProgressRespVO();v.setTaskId(id);v.setTaskStatus(task.getTaskStatus());v.setPlanQuantity(task.getPlanQuantity());v.setOperationTotal((long)list.size());v.setOperationCompleted(list.stream().filter(d->SceneOperationStatusEnum.COMPLETED.getStatus().equals(d.getDetailStatus())).count());
        list.stream().filter(d->!SceneOperationStatusEnum.COMPLETED.getStatus().equals(d.getDetailStatus())).findFirst().ifPresent(d->{v.setCurrentOperationId(d.getId());v.setCurrentProcessName(d.getProcessName());});return v;}
    private SceneProductionTaskEntity requireTask(Long id){return taskRepository.findByIdAndDeletedFalse(id).orElseThrow(()->new ServiceException(SceneErrorCodeConstants.TASK_NOT_EXISTS));}
    private void invalid(){throw new ServiceException(SceneErrorCodeConstants.TASK_STATUS_INVALID);}
    private void validateTime(SceneProductionTaskSaveReqVO r){if(r.getPlanEndTime().isBefore(r.getPlanStartTime()))throw new ServiceException(SceneErrorCodeConstants.PARAM_VALUE_INVALID);}
    private void copyRequest(SceneProductionTaskEntity t,SceneProductionTaskSaveReqVO r){t.setWorkOrderId(r.getWorkOrderId());t.setLineId(r.getLineId());t.setShiftId(r.getShiftId());t.setPlanDate(r.getPlanDate());t.setPlanQuantity(r.getPlanQuantity());t.setPlanStartTime(r.getPlanStartTime());t.setPlanEndTime(r.getPlanEndTime());}
    private void insertLog(Long id,int type,Integer from,Integer to,String reason){SceneTaskOperateLogEntity l=new SceneTaskOperateLogEntity();l.setTaskId(id);l.setOperateType(type);l.setFromStatus(from);l.setToStatus(to);l.setReason(reason);l.setTerminalType(1);l.setOperatorId(SecurityContextHolder.getRequiredLoginUserId());l.setOperateTime(LocalDateTime.now());logRepository.save(l);}
    private SceneProductionTaskRespVO toResp(SceneProductionTaskEntity t){SceneProductionTaskRespVO v=new SceneProductionTaskRespVO();v.setId(t.getId());v.setTaskNo(t.getTaskNo());v.setWorkOrderId(t.getWorkOrderId());v.setWorkOrderNo(t.getWorkOrderNo());v.setProductId(t.getProductId());v.setProductCode(t.getProductCode());v.setProductName(t.getProductName());v.setBatchNo(t.getBatchNo());v.setRoutingId(t.getRoutingId());v.setRoutingCode(t.getRoutingCode());v.setRoutingVersion(t.getRoutingVersion());v.setWorkshopId(t.getWorkshopId());v.setWorkshopName(t.getWorkshopName());v.setLineId(t.getLineId());v.setLineName(t.getLineName());v.setShiftId(t.getShiftId());v.setPlanDate(t.getPlanDate());v.setPlanQuantity(t.getPlanQuantity());v.setInputQuantity(t.getInputQuantity());v.setGoodQuantity(t.getGoodQuantity());v.setDefectQuantity(t.getDefectQuantity());v.setReworkQuantity(t.getReworkQuantity());v.setFinishQuantity(t.getFinishQuantity());v.setPlanStartTime(t.getPlanStartTime());v.setPlanEndTime(t.getPlanEndTime());v.setActualStartTime(t.getActualStartTime());v.setActualEndTime(t.getActualEndTime());v.setTaskStatus(t.getTaskStatus());v.setPauseReason(t.getPauseReason());return v;}
}
