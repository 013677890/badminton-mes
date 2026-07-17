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
        // 普通页面先解析当前登录人的数据范围，禁止通过请求参数越权查看其他车间或产线。
        ReportDataScope scope = dataScopeService.resolve(reqVO.getWorkshopId(), reqVO.getLineId());
        return buildOverview(reqVO, scope);
    }

    @Override
    @Transactional(readOnly = true)
    public RealtimeProductionRespVO.Overview overviewForKanban(RealtimeReportQueryReqVO reqVO) {
        // 看板由受控入口调用，使用入口已确定的范围，不重复读取当前用户页面筛选状态。
        ReportDataScope scope = new ReportDataScope(reqVO.getWorkshopId(), reqVO.getLineId());
        return buildOverview(reqVO, scope);
    }

    private RealtimeProductionRespVO.Overview buildOverview(RealtimeReportQueryReqVO reqVO,
                                                              ReportDataScope scope) {
        // 任务明细和设备/安灯支持数据分别查询，再在服务层聚合为前端所需的一个概览对象。
        List<RealtimeTask> rows = query(reqVO, scope);
        RealtimeProductionRespVO.Overview result = new RealtimeProductionRespVO.Overview();
        result.setActiveTaskCount(rows.stream().filter(row -> row.taskStatus() == 3).count());
        // 状态码由生产任务状态枚举定义：3 表示生产中，4 表示暂停；统计逻辑只负责汇总已过滤数据。
        result.setPausedTaskCount(rows.stream().filter(row -> row.taskStatus() == 4).count());
        result.setAbnormalBatchCount(rows.stream().filter(RealtimeTask::abnormal).count());
        result.setPlanQuantity(rows.stream().mapToLong(row -> zero(row.planQuantity())).sum());
        result.setInputQuantity(rows.stream().mapToLong(row -> zero(row.inputQuantity())).sum());
        result.setGoodQuantity(rows.stream().mapToLong(row -> zero(row.goodQuantity())).sum());
        result.setDefectQuantity(rows.stream().mapToLong(row -> zero(row.defectQuantity())).sum());
        // 设备和安灯数据来自独立表，避免任务表联表后因一对多关系重复放大生产数量。
        RealtimeSupport support = repository.loadRealtimeSupport(scope.workshopId(), scope.lineId());
        result.setEquipmentTotalCount(support.equipmentTotalCount());
        result.setRunningEquipmentCount(support.runningEquipmentCount());
        result.setUnavailableEquipmentCount(support.unavailableEquipmentCount());
        result.setOpenAndonCount(support.openAndonCount());
        result.setCriticalAndonCount(support.criticalAndonCount());
        result.setLastRefreshTime(LocalDateTime.now());
        result.setDataStatus("PARTIAL");
        // OEE 事实表尚未接入，明确返回降级状态和提示，避免前端把缺失数据当成 0 展示。
        result.setWarnings(List.of("EQUIPMENT_OEE：已接入设备状态，C组当前未提供OEE事实表，OEE指标暂不展示"));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RealtimeProductionRespVO.Task> tasks(RealtimeReportQueryReqVO reqVO) {
        // 明细接口复用同一数据范围和查询入口，保证概览数量与任务列表筛选口径一致。
        ReportDataScope scope = dataScopeService.resolve(reqVO.getWorkshopId(), reqVO.getLineId());
        return query(reqVO, scope).stream().map(this::toTask).toList();
    }

    private List<RealtimeTask> query(RealtimeReportQueryReqVO reqVO, ReportDataScope scope) {
        // Repository 负责 SQL 和参数绑定，Service 只传递已授权范围及可选产品条件。
        return repository.listRealtimeTasks(scope.workshopId(), scope.lineId(), reqVO.getProductId());
    }

    private RealtimeProductionRespVO.Task toTask(RealtimeTask row) {
        // 显式逐字段转换，避免直接暴露 JDBC 行对象，也便于后续调整接口字段而不影响查询层。
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
        // 数据库聚合或历史数据可能产生 null，报表展示统一按 0 参与计算。
        return value == null ? 0 : value;
    }
}
