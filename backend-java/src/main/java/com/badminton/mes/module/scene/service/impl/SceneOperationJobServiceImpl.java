package com.badminton.mes.module.scene.service.impl;

import java.time.LocalDateTime;import java.util.*;
import com.badminton.mes.common.core.PageResult;import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.*;import com.badminton.mes.module.barcode.service.BarcodeSceneService;
import com.badminton.mes.module.scene.constants.*;import com.badminton.mes.module.scene.controller.vo.*;
import com.badminton.mes.module.scene.dal.entity.*;import com.badminton.mes.module.scene.dal.repository.*;
import com.badminton.mes.module.scene.enums.*;import com.badminton.mes.module.scene.service.*;
import org.springframework.data.domain.*;import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 工序作业 Service。 @author 刘涵 */
@Service
public class SceneOperationJobServiceImpl implements SceneOperationJobService {
 private final SceneDispatchDetailRepository detailRepository;private final SceneDispatchOrderRepository orderRepository;
 private final SceneProductionTaskRepository taskRepository;private final SceneBatchStatusRepository batchRepository;
 private final SceneBatchStatusHistoryRepository statusHistoryRepository;private final SceneBatchProcessHistoryRepository processHistoryRepository;
 private final BarcodeSceneService barcodeService;private final SceneProductionParameterService parameterService;private final SceneDataScopeService dataScope;
 public SceneOperationJobServiceImpl(SceneDispatchDetailRepository detailRepository,SceneDispatchOrderRepository orderRepository,
   SceneProductionTaskRepository taskRepository,SceneBatchStatusRepository batchRepository,
   SceneBatchStatusHistoryRepository statusHistoryRepository,SceneBatchProcessHistoryRepository processHistoryRepository,
   BarcodeSceneService barcodeService,SceneProductionParameterService parameterService,SceneDataScopeService dataScope){
  this.detailRepository=detailRepository;this.orderRepository=orderRepository;this.taskRepository=taskRepository;this.batchRepository=batchRepository;
  this.statusHistoryRepository=statusHistoryRepository;this.processHistoryRepository=processHistoryRepository;this.barcodeService=barcodeService;this.parameterService=parameterService;this.dataScope=dataScope;}
 @Override @Transactional(readOnly=true) public PageResult<SceneDispatchDetailRespVO> page(SceneOperationJobPageReqVO req){
  // Repository 未扩展 Specification，按当前任务规模用内存过滤前先限定最多 100 条
  List<SceneDispatchDetailEntity> all=detailRepository.findAll(Sort.by("id").descending()).stream().filter(d->matches(req,d)).filter(this::accessible).toList();
  int from=Math.min((req.getPageNo()-1)*req.getPageSize(),all.size());int to=Math.min(from+req.getPageSize(),all.size());
  return PageResult.of(all.subList(from,to).stream().map(this::toResp).toList(),(long)all.size(),req.getPageNo(),req.getPageSize());}
 @Override @Transactional(readOnly=true) public List<SceneDispatchDetailRespVO> my(){LoginUser u=SecurityContextHolder.getRequiredLoginUser();return detailRepository.findAll(Sort.by("seq")).stream().filter(d->d.getUserId()==null||Objects.equals(d.getUserId(),u.getUserId())).filter(this::accessible).map(this::toResp).toList();}
 @Override @Transactional(readOnly=true) public SceneDispatchDetailRespVO get(Long id){SceneDispatchDetailEntity d=require(id);check(d);return toResp(d);}
 @Override @Transactional(rollbackFor=Exception.class) public void scan(Long id,SceneOperationScanReqVO req){SceneDispatchDetailEntity d=require(id);SceneProductionTaskEntity t=check(d);
  barcodeService.validateAndRecordUse(req.getBarcodeValue(),t.getId(),t.getProductId(),t.getBatchNo(),d.getProcessId(),SecurityContextHolder.getRequiredLoginUserId(),req.getEquipmentId(),1);
  SceneBatchStatusEntity b=ensureBatch(t,d);insertProcessHistory(b,d,1,null);}
 @Override @Transactional(rollbackFor=Exception.class) public void start(Long id){SceneDispatchDetailEntity d=require(id);SceneProductionTaskEntity t=check(d);
  boolean resuming=Boolean.TRUE.equals(d.getPaused());
  if(!SceneOperationStatusEnum.PENDING.getStatus().equals(d.getDetailStatus())&&!Boolean.TRUE.equals(d.getPaused()))invalid();
  if(Boolean.TRUE.equals(d.getScanRequired())&&!processHistoryRepository.existsByDispatchDetailIdAndActionTypeAndDeletedFalse(id,1))throw new ServiceException(SceneErrorCodeConstants.OPERATION_SCAN_REQUIRED);
  validateSequence(d,t);d.setDetailStatus(SceneOperationStatusEnum.IN_PROGRESS.getStatus());d.setPaused(false);d.setPauseReason(null);if(d.getActualStartTime()==null)d.setActualStartTime(LocalDateTime.now());detailRepository.save(d);
  SceneDispatchOrderEntity o=orderRepository.findByIdAndDeletedFalse(d.getDispatchId()).orElseThrow(()->new ServiceException(SceneErrorCodeConstants.DISPATCH_NOT_EXISTS));
  if(SceneDispatchStatusEnum.CONFIRMED.getStatus().equals(o.getDispatchStatus()))orderRepository.transition(o.getId(),1,2);
  if(SceneTaskStatusEnum.RELEASED.getStatus().equals(t.getTaskStatus()))taskRepository.transition(t.getId(),2,3,null);
  SceneBatchStatusEntity b=ensureBatch(t,d);b.setCurrentProcessId(d.getProcessId());b.setCurrentProcessName(d.getProcessName());batchRepository.save(b);insertProcessHistory(b,d,resuming?4:2,null);}
 @Override @Transactional(rollbackFor=Exception.class) public void pause(Long id,String reason){SceneDispatchDetailEntity d=require(id);SceneProductionTaskEntity t=check(d);if(!SceneOperationStatusEnum.IN_PROGRESS.getStatus().equals(d.getDetailStatus())||Boolean.TRUE.equals(d.getPaused()))invalid();d.setPaused(true);d.setPauseReason(reason);detailRepository.save(d);insertProcessHistory(ensureBatch(t,d),d,3,reason);}
 @Override @Transactional(rollbackFor=Exception.class) public void finish(Long id){SceneDispatchDetailEntity d=require(id);SceneProductionTaskEntity t=check(d);if(!SceneOperationStatusEnum.IN_PROGRESS.getStatus().equals(d.getDetailStatus())||Boolean.TRUE.equals(d.getPaused()))invalid();d.setDetailStatus(SceneOperationStatusEnum.COMPLETED.getStatus());d.setActualEndTime(LocalDateTime.now());detailRepository.save(d);SceneBatchStatusEntity b=ensureBatch(t,d);insertProcessHistory(b,d,5,null);
  List<SceneDispatchDetailEntity> list=detailRepository.findByDispatchIdAndDeletedFalseOrderBySeqAsc(d.getDispatchId());Optional<SceneDispatchDetailEntity> next=list.stream().filter(x->!SceneOperationStatusEnum.COMPLETED.getStatus().equals(x.getDetailStatus())).findFirst();
  if(next.isPresent()){b.setCurrentProcessId(next.get().getProcessId());b.setCurrentProcessName(next.get().getProcessName());batchRepository.save(b);}
  else{SceneDispatchOrderEntity o=orderRepository.findByIdAndDeletedFalse(d.getDispatchId()).orElseThrow();orderRepository.transition(o.getId(),2,3);}}
 private void validateSequence(SceneDispatchDetailEntity d,SceneProductionTaskEntity t){if(!Boolean.TRUE.equals(d.getKeyProcess())&&!Boolean.TRUE.equals(d.getInspect())&&allowSkip(t))return;boolean incomplete=detailRepository.findByDispatchIdAndDeletedFalseOrderBySeqAsc(d.getDispatchId()).stream().anyMatch(x->x.getSeq()<d.getSeq()&&!SceneOperationStatusEnum.COMPLETED.getStatus().equals(x.getDetailStatus()));if(incomplete)throw new ServiceException(SceneErrorCodeConstants.OPERATION_SEQUENCE_INVALID);}
 private boolean allowSkip(SceneProductionTaskEntity t){SceneEffectiveParameterReqVO r=new SceneEffectiveParameterReqVO();r.setParamCode(SceneParameterCodes.ALLOW_SKIP_PROCESS);r.setWorkshopId(t.getWorkshopId());r.setLineId(t.getLineId());r.setProductId(t.getProductId());return "1".equals(parameterService.getEffectiveParameter(r).getParamValue());}
 private SceneBatchStatusEntity ensureBatch(SceneProductionTaskEntity t,SceneDispatchDetailEntity d){return batchRepository.findByBatchNoAndDeletedFalse(t.getBatchNo()).orElseGet(()->{SceneBatchStatusEntity b=new SceneBatchStatusEntity();b.setBatchNo(t.getBatchNo());b.setTaskId(t.getId());b.setProductId(t.getProductId());b.setCurrentProcessId(d.getProcessId());b.setCurrentProcessName(d.getProcessName());b.setBatchStatus(SceneBatchStatusEnum.IN_PROCESS.getStatus());b.setAbnormal(false);batchRepository.saveAndFlush(b);SceneBatchStatusHistoryEntity h=new SceneBatchStatusHistoryEntity();h.setBatchStatusId(b.getId());h.setTaskId(t.getId());h.setBatchNo(t.getBatchNo());h.setToStatus(b.getBatchStatus());h.setProcessId(d.getProcessId());h.setChangeReason("首道工序进入生产");h.setOperatorId(SecurityContextHolder.getRequiredLoginUserId());h.setOperateTime(LocalDateTime.now());statusHistoryRepository.save(h);return b;});}
 private void insertProcessHistory(SceneBatchStatusEntity b,SceneDispatchDetailEntity d,int action,String reason){SceneBatchProcessHistoryEntity h=new SceneBatchProcessHistoryEntity();h.setBatchStatusId(b.getId());h.setTaskId(d.getTaskId());h.setDispatchDetailId(d.getId());h.setBatchNo(b.getBatchNo());h.setProcessId(d.getProcessId());h.setProcessCode(d.getProcessCode());h.setProcessName(d.getProcessName());h.setActionType(action);h.setOperatorId(SecurityContextHolder.getRequiredLoginUserId());h.setActionReason(reason);h.setOperateTime(LocalDateTime.now());processHistoryRepository.save(h);}
 private SceneDispatchDetailEntity require(Long id){return detailRepository.findByIdAndDeletedFalse(id).orElseThrow(()->new ServiceException(SceneErrorCodeConstants.OPERATION_NOT_EXISTS));}
 private SceneProductionTaskEntity check(SceneDispatchDetailEntity d){SceneProductionTaskEntity t=taskRepository.findByIdAndDeletedFalse(d.getTaskId()).orElseThrow(()->new ServiceException(SceneErrorCodeConstants.TASK_NOT_EXISTS));dataScope.check(t.getWorkshopId(),t.getLineId());LoginUser u=SecurityContextHolder.getRequiredLoginUser();if(d.getUserId()!=null&&!Objects.equals(d.getUserId(),u.getUserId())&&!u.getRoleCodes().contains(RoleCodeConstants.ADMIN)&&!u.getRoleCodes().contains(RoleCodeConstants.WORKSHOP_MANAGER)&&!u.getRoleCodes().contains(RoleCodeConstants.TEAM_LEADER))throw new ServiceException(SceneErrorCodeConstants.DATA_SCOPE_DENIED);return t;}
 private boolean accessible(SceneDispatchDetailEntity d){try{check(d);return true;}catch(ServiceException e){return false;}}
 private boolean matches(SceneOperationJobPageReqVO r,SceneDispatchDetailEntity d){return(r.getTaskId()==null||r.getTaskId().equals(d.getTaskId()))&&(r.getUserId()==null||r.getUserId().equals(d.getUserId()))&&(r.getStationId()==null||r.getStationId().equals(d.getStationId()))&&(r.getEquipmentId()==null||r.getEquipmentId().equals(d.getEquipmentId()))&&(r.getDetailStatus()==null||r.getDetailStatus().equals(d.getDetailStatus()));}
 private void invalid(){throw new ServiceException(SceneErrorCodeConstants.OPERATION_STATUS_INVALID);}
 private SceneDispatchDetailRespVO toResp(SceneDispatchDetailEntity d){SceneDispatchDetailRespVO v=new SceneDispatchDetailRespVO();v.setId(d.getId());v.setProcessId(d.getProcessId());v.setProcessCode(d.getProcessCode());v.setProcessName(d.getProcessName());v.setSeq(d.getSeq());v.setKeyProcess(d.getKeyProcess());v.setInspect(d.getInspect());v.setScanRequired(d.getScanRequired());v.setSopId(d.getSopId());v.setSopCode(d.getSopCode());v.setSopName(d.getSopName());v.setSopVersion(d.getSopVersion());v.setStationId(d.getStationId());v.setUserId(d.getUserId());v.setEquipmentId(d.getEquipmentId());v.setPlanQuantity(d.getPlanQuantity());v.setDetailStatus(d.getDetailStatus());v.setPaused(d.getPaused());v.setPauseReason(d.getPauseReason());v.setActualStartTime(d.getActualStartTime());v.setActualEndTime(d.getActualEndTime());return v;}
}
