package com.badminton.mes.module.report.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.badminton.mes.module.report.controller.vo.RealtimeProductionRespVO;
import com.badminton.mes.module.report.controller.vo.RealtimeReportQueryReqVO;
import com.badminton.mes.module.report.dal.ReportQueryRepository;
import com.badminton.mes.module.report.dal.ReportQueryRows.RealtimeTask;
import com.badminton.mes.module.report.dal.ReportQueryRows.RealtimeSupport;
import com.badminton.mes.module.report.service.RealtimeProductionService;
import com.badminton.mes.module.report.service.ReportDataScopeService;
import com.badminton.mes.module.report.service.ReportDataScopeService.ReportDataScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 实时生产查询实现。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@Service
public class RealtimeProductionServiceImpl implements RealtimeProductionService {

    private final ReportQueryRepository repository;
    private final ReportDataScopeService dataScopeService;

    public RealtimeProductionServiceImpl(ReportQueryRepository repository,
                                         ReportDataScopeService dataScopeService) {
        this.repository = repository;
        this.dataScopeService = dataScopeService;
    }

    @Override
    @Transactional(readOnly = true)
    public RealtimeProductionRespVO.Overview overview(RealtimeReportQueryReqVO reqVO) {
        ReportDataScope scope = dataScopeService.resolve(reqVO.getWorkshopId(), reqVO.getLineId());
        return buildOverview(reqVO, scope);
    }

    @Override
    @Transactional(readOnly = true)
    public RealtimeProductionRespVO.Overview overviewForKanban(RealtimeReportQueryReqVO reqVO) {
        ReportDataScope scope = new ReportDataScope(reqVO.getWorkshopId(), reqVO.getLineId());
        return buildOverview(reqVO, scope);
    }

    private RealtimeProductionRespVO.Overview buildOverview(RealtimeReportQueryReqVO reqVO,
                                                              ReportDataScope scope) {
        List<RealtimeTask> rows = query(reqVO, scope);
        RealtimeProductionRespVO.Overview result = new RealtimeProductionRespVO.Overview();
        result.setActiveTaskCount(rows.stream().filter(row -> row.taskStatus() == 3).count());
        result.setPausedTaskCount(rows.stream().filter(row -> row.taskStatus() == 4).count());
        result.setAbnormalBatchCount(rows.stream().filter(RealtimeTask::abnormal).count());
        result.setPlanQuantity(rows.stream().mapToLong(row -> zero(row.planQuantity())).sum());
        result.setInputQuantity(rows.stream().mapToLong(row -> zero(row.inputQuantity())).sum());
        result.setGoodQuantity(rows.stream().mapToLong(row -> zero(row.goodQuantity())).sum());
        result.setDefectQuantity(rows.stream().mapToLong(row -> zero(row.defectQuantity())).sum());
        RealtimeSupport support = repository.loadRealtimeSupport(scope.workshopId(), scope.lineId());
        result.setEquipmentTotalCount(support.equipmentTotalCount());
        result.setRunningEquipmentCount(support.runningEquipmentCount());
        result.setUnavailableEquipmentCount(support.unavailableEquipmentCount());
        result.setOpenAndonCount(support.openAndonCount());
        result.setCriticalAndonCount(support.criticalAndonCount());
        result.setLastRefreshTime(LocalDateTime.now());
        result.setDataStatus("PARTIAL");
        result.setWarnings(List.of("EQUIPMENT_OEE：已接入设备状态，C组当前未提供OEE事实表，OEE指标暂不展示"));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RealtimeProductionRespVO.Task> tasks(RealtimeReportQueryReqVO reqVO) {
        ReportDataScope scope = dataScopeService.resolve(reqVO.getWorkshopId(), reqVO.getLineId());
        return query(reqVO, scope).stream().map(this::toTask).toList();
    }

    private List<RealtimeTask> query(RealtimeReportQueryReqVO reqVO, ReportDataScope scope) {
        return repository.listRealtimeTasks(scope.workshopId(), scope.lineId(), reqVO.getProductId());
    }

    private RealtimeProductionRespVO.Task toTask(RealtimeTask row) {
        RealtimeProductionRespVO.Task result = new RealtimeProductionRespVO.Task();
        result.setTaskId(row.taskId());
        result.setTaskNo(row.taskNo());
        result.setWorkOrderNo(row.workOrderNo());
        result.setProductId(row.productId());
        result.setProductName(row.productName());
        result.setBatchNo(row.batchNo());
        result.setWorkshopId(row.workshopId());
        result.setWorkshopName(row.workshopName());
        result.setLineId(row.lineId());
        result.setLineName(row.lineName());
        result.setPlanQuantity(row.planQuantity());
        result.setInputQuantity(row.inputQuantity());
        result.setGoodQuantity(row.goodQuantity());
        result.setDefectQuantity(row.defectQuantity());
        result.setFinishQuantity(row.finishQuantity());
        result.setTaskStatus(row.taskStatus());
        result.setAbnormal(row.abnormal());
        result.setActualStartTime(row.actualStartTime());
        result.setUpdateTime(row.updateTime());
        return result;
    }

    private int zero(Integer value) {
        return value == null ? 0 : value;
    }
}
