package com.badminton.mes.module.integration.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.integration.constants.IntegrationErrorCodeConstants;
import com.badminton.mes.module.integration.controller.vo.ErpCraftPendingRespVO;
import com.badminton.mes.module.integration.controller.vo.ErpCraftPendingPageReqVO;
import com.badminton.mes.module.integration.controller.vo.ErpCraftSyncReqVO;
import com.badminton.mes.module.integration.controller.vo.ErpCraftSyncRespVO;
import com.badminton.mes.module.integration.controller.vo.ErpSyncLogPageReqVO;
import com.badminton.mes.module.integration.controller.vo.ErpTaskSyncReqVO;
import com.badminton.mes.module.integration.controller.vo.ErpTaskSyncRespVO;
import com.badminton.mes.module.integration.controller.vo.IntegrationWriteLogRespVO;
import com.badminton.mes.module.integration.dal.entity.ErpCraftPendingEntity;
import com.badminton.mes.module.integration.dal.entity.IntegrationWriteLogEntity;
import com.badminton.mes.module.integration.dal.repository.ErpSyncLogSpecifications;
import com.badminton.mes.module.integration.dal.repository.ErpCraftPendingRepository;
import com.badminton.mes.module.integration.dal.repository.ErpCraftPendingSpecifications;
import com.badminton.mes.module.integration.dal.repository.IntegrationWriteLogRepository;
import com.badminton.mes.module.integration.enums.IntegrationInterfaceTypeEnum;
import com.badminton.mes.module.integration.enums.ErpCraftPendingStatusEnum;
import com.badminton.mes.module.integration.service.ErpCraftSyncCommandService;
import com.badminton.mes.module.integration.service.ErpDataSource;
import com.badminton.mes.module.integration.service.ErpSyncService;
import com.badminton.mes.module.integration.service.ErpTaskSyncCommandService;
import com.badminton.mes.module.integration.service.IntegrationAuditService;
import com.badminton.mes.module.integration.service.dto.ErpCraftDTO;
import com.badminton.mes.module.integration.service.dto.ErpCraftSyncResult;
import com.badminton.mes.module.integration.service.dto.ErpTaskDTO;
import com.badminton.mes.module.integration.service.dto.IntegrationCommandResult;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * ERP 同步门面实现，编排批量同步流程并汇总计数。
 *
 * <p>任务同步逐条独立事务，失败由门面用独立事务记录日志；
 * 工艺同步由命令服务内部处理成功与异常，门面仅汇总。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Service
public class ErpSyncServiceImpl implements ErpSyncService {

    private static final String INVALID_BUSINESS_KEY = "INVALID_SOURCE_DATA";

    private final ErpDataSource erpDataSource;

    private final ErpTaskSyncCommandService taskSyncCommandService;

    private final ErpCraftSyncCommandService craftSyncCommandService;

    private final IntegrationAuditService auditService;

    private final IntegrationWriteLogRepository writeLogRepository;

    private final ErpCraftPendingRepository pendingRepository;

    /**
     * 构造 ERP 同步门面。
     *
     * @param erpDataSource           ERP 数据源
     * @param taskSyncCommandService  任务同步命令服务
     * @param craftSyncCommandService 工艺同步命令服务
     * @param auditService            接口审计服务
     * @param writeLogRepository      写入日志 Repository
     */
    @org.springframework.beans.factory.annotation.Autowired
    public ErpSyncServiceImpl(ErpDataSource erpDataSource,
                              ErpTaskSyncCommandService taskSyncCommandService,
                              ErpCraftSyncCommandService craftSyncCommandService,
                              IntegrationAuditService auditService,
                               IntegrationWriteLogRepository writeLogRepository,
                               ErpCraftPendingRepository pendingRepository) {
        this.erpDataSource = erpDataSource;
        this.taskSyncCommandService = taskSyncCommandService;
        this.craftSyncCommandService = craftSyncCommandService;
        this.auditService = auditService;
        this.writeLogRepository = writeLogRepository;
        this.pendingRepository = pendingRepository;
    }

    /** 兼容既有聚焦单测的构造入口。 */
    public ErpSyncServiceImpl(ErpDataSource erpDataSource,
                              ErpTaskSyncCommandService taskSyncCommandService,
                              ErpCraftSyncCommandService craftSyncCommandService,
                              IntegrationAuditService auditService,
                              IntegrationWriteLogRepository writeLogRepository) {
        this(erpDataSource, taskSyncCommandService, craftSyncCommandService,
                auditService, writeLogRepository, null);
    }

    @Override
    public ErpTaskSyncRespVO syncErpTasks(ErpTaskSyncReqVO reqVO) {
        String sourceSystem = resolveSourceSystem(reqVO.getSourceSystem());
        validateTaskTimeRange(reqVO);
        List<ErpTaskDTO> tasks = filterTasks(
                erpDataSource.fetchTasks(), reqVO.getErpOrderNo(),
                reqVO.getStartTime(), reqVO.getEndTime());

        ErpTaskSyncRespVO response = new ErpTaskSyncRespVO();
        response.setSourceSystem(sourceSystem);
        response.setTotalCount(tasks.size());
        List<ErpTaskSyncRespVO.Detail> details = new ArrayList<>(tasks.size());

        for (ErpTaskDTO task : tasks) {
            String snapshot = auditService.serializeRequest(task);
            syncTaskItem(task, sourceSystem, snapshot, response, details);
        }
        response.setDetails(details);
        return response;
    }

    @Override
    public PageResult<IntegrationWriteLogRespVO> getErpTaskSyncLogPage(ErpSyncLogPageReqVO reqVO) {
        Specification<IntegrationWriteLogEntity> specification =
                ErpSyncLogSpecifications.erpTaskSyncLogPage(reqVO);
        long total = writeLogRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        int pageSize = reqVO.getPageSize();
        int pageNo = normalizePageNo(reqVO.getPageNo(), pageSize, total);
        PageRequest pageRequest = PageRequest.of(
                pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<IntegrationWriteLogEntity> page =
                writeLogRepository.findAll(specification, pageRequest);
        return PageResult.of(page.getContent().stream()
                        .map(ErpSyncResponseConverter::toLogResponse)
                        .toList(),
                total, pageNo, pageSize);
    }

    @Override
    public ErpCraftSyncRespVO syncErpCrafts(ErpCraftSyncReqVO reqVO) {
        String sourceSystem = resolveSourceSystem(reqVO.getSourceSystem());
        List<ErpCraftDTO> crafts = erpDataSource.fetchCrafts();

        ErpCraftSyncRespVO response = new ErpCraftSyncRespVO();
        response.setSourceSystem(sourceSystem);
        response.setTotalCount(crafts.size());
        List<ErpCraftPendingRespVO> pendingItems = new ArrayList<>(crafts.size());

        for (ErpCraftDTO craft : crafts) {
            String snapshot = auditService.serializeRequest(craft);
            syncCraftItem(craft, sourceSystem, snapshot, response, pendingItems);
        }
        response.setPendingItems(pendingItems);
        return response;
    }

    @Override
    public Long confirmPendingCraft(Long id) {
        return craftSyncCommandService.confirmCraft(id);
    }

    @Override
    public PageResult<ErpCraftPendingRespVO> getPendingCraftPage(
            ErpCraftPendingPageReqVO reqVO) {
        Specification<ErpCraftPendingEntity> specification =
                ErpCraftPendingSpecifications.page(reqVO);
        long total = pendingRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }
        int pageNo = normalizePageNo(reqVO.getPageNo(), reqVO.getPageSize(), total);
        Page<ErpCraftPendingEntity> page = pendingRepository.findAll(
                specification, PageRequest.of(pageNo - 1, reqVO.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "id")));
        return PageResult.of(page.getContent().stream()
                        .map(ErpSyncResponseConverter::toPendingResponse).toList(),
                total, pageNo, reqVO.getPageSize());
    }

    @Override
    public void rejectPendingCraft(Long id, String reason) {
        craftSyncCommandService.rejectCraft(id, reason);
    }

    /**
     * 处理单条 ERP 任务，业务异常仅影响当前数据。
     *
     * @param task         ERP 任务数据
     * @param sourceSystem 来源系统
     * @param snapshot     请求快照
     * @param response     批量响应
     * @param details      任务明细
     */
    private void syncTaskItem(ErpTaskDTO task,
                              String sourceSystem,
                              String snapshot,
                              ErpTaskSyncRespVO response,
                              List<ErpTaskSyncRespVO.Detail> details) {
        String businessKey = resolveTaskBusinessKey(task);
        try {
            IntegrationCommandResult result = taskSyncCommandService
                    .syncTask(task, snapshot, sourceSystem);
            collectTaskResult(businessKey, result, response, details);
        } catch (ServiceException exception) {
            Optional<IntegrationCommandResult> concurrentResult =
                    findConcurrentTaskResult(task, sourceSystem, snapshot, exception);
            if (concurrentResult.isPresent()) {
                collectTaskResult(businessKey, concurrentResult.get(), response, details);
                return;
            }
            auditService.recordFailure(
                    IntegrationInterfaceTypeEnum.ERP_TASK_SYNC,
                    sourceSystem, businessKey, snapshot,
                    null, null, exception.getErrorCode(), exception.getMessage());
            response.setFailureCount(response.getFailureCount() + 1);
            details.add(ErpSyncResponseConverter.toTaskFailureDetail(
                    businessKey, exception));
        }
    }

    /**
     * 查询并记录任务唯一键竞争中的已提交获胜记录。
     *
     * @param task         ERP 任务数据
     * @param sourceSystem 来源系统
     * @param snapshot     请求快照
     * @param exception    命令异常
     * @return 并发重复结果；非唯一键竞争或获胜记录不可见时为空
     */
    private Optional<IntegrationCommandResult> findConcurrentTaskResult(
            ErpTaskDTO task,
            String sourceSystem,
            String snapshot,
            ServiceException exception) {
        if (!isErrorCode(exception, IntegrationErrorCodeConstants.EXTERNAL_WORK_ORDER_DUPLICATE)
                || task == null
                || !StringUtils.hasText(task.erpOrderNo())) {
            return Optional.empty();
        }
        String erpOrderNo = task.erpOrderNo().trim();
        Optional<WorkOrderEntity> existing = taskSyncCommandService
                .findSyncedTask(sourceSystem, erpOrderNo);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        WorkOrderEntity workOrder = existing.get();
        Long logId = auditService.recordDuplicate(
                IntegrationInterfaceTypeEnum.ERP_TASK_SYNC,
                sourceSystem, erpOrderNo, snapshot,
                workOrder.getId(), workOrder.getWorkOrderNo());
        return Optional.of(new IntegrationCommandResult(
                workOrder.getId(), workOrder.getWorkOrderNo(), true, logId));
    }

    /**
     * 汇总单条任务同步结果。
     *
     * @param businessKey ERP 任务业务键
     * @param result      命令结果
     * @param response    批量响应
     * @param details     任务明细
     */
    private void collectTaskResult(String businessKey,
                                   IntegrationCommandResult result,
                                   ErpTaskSyncRespVO response,
                                   List<ErpTaskSyncRespVO.Detail> details) {
        if (result.duplicate()) {
            response.setDuplicateCount(response.getDuplicateCount() + 1);
        } else {
            response.setSuccessCount(response.getSuccessCount() + 1);
        }
        details.add(ErpSyncResponseConverter.toTaskDetail(businessKey, result));
    }

    /**
     * 处理单条 ERP 工艺，业务异常仅影响当前数据。
     *
     * @param craft        ERP 工艺数据
     * @param sourceSystem 来源系统
     * @param snapshot     请求快照
     * @param response     批量响应
     * @param pendingItems 待确认响应列表
     */
    private void syncCraftItem(ErpCraftDTO craft,
                               String sourceSystem,
                               String snapshot,
                               ErpCraftSyncRespVO response,
                               List<ErpCraftPendingRespVO> pendingItems) {
        String businessKey = resolveCraftBusinessKey(craft);
        try {
            ErpCraftSyncResult result = craftSyncCommandService
                    .syncCraft(craft, snapshot, sourceSystem);
            collectCraftResult(result, response, pendingItems);
        } catch (ServiceException exception) {
            Optional<ErpCraftSyncResult> concurrentResult =
                    findConcurrentCraftResult(craft, sourceSystem, snapshot, exception);
            if (concurrentResult.isPresent()) {
                collectCraftResult(concurrentResult.get(), response, pendingItems);
                return;
            }
            auditService.recordFailure(
                    IntegrationInterfaceTypeEnum.ERP_CRAFT_SYNC,
                    sourceSystem, businessKey, snapshot,
                    null, null, exception.getErrorCode(), exception.getMessage());
            response.setFailureCount(response.getFailureCount() + 1);
        }
    }

    /**
     * 查询并记录工艺唯一键竞争中的已提交获胜记录。
     *
     * @param craft        ERP 工艺数据
     * @param sourceSystem 来源系统
     * @param snapshot     请求快照
     * @param exception    命令异常
     * @return 并发重复结果；非唯一键竞争或获胜记录不可见时为空
     */
    private Optional<ErpCraftSyncResult> findConcurrentCraftResult(
            ErpCraftDTO craft,
            String sourceSystem,
            String snapshot,
            ServiceException exception) {
        if (!isErrorCode(exception, IntegrationErrorCodeConstants.ERP_CRAFT_DUPLICATE)
                || craft == null
                || !StringUtils.hasText(craft.erpRoutingCode())
                || !StringUtils.hasText(craft.erpRoutingVersion())) {
            return Optional.empty();
        }
        String routingCode = craft.erpRoutingCode().trim();
        String routingVersion = craft.erpRoutingVersion().trim();
        String businessKey = routingCode + ":" + routingVersion;
        Optional<ErpCraftPendingEntity> existing = craftSyncCommandService
                .findSyncedCraft(sourceSystem, routingCode, routingVersion);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        ErpCraftPendingEntity pending = existing.get();
        Long logId = auditService.recordDuplicate(
                IntegrationInterfaceTypeEnum.ERP_CRAFT_SYNC,
                sourceSystem, businessKey, snapshot,
                pending.getId(), businessKey);
        return Optional.of(new ErpCraftSyncResult(
                null, true, logId, null, null));
    }

    /**
     * 汇总单条工艺同步结果。
     *
     * @param result       命令结果
     * @param response     批量响应
     * @param pendingItems 待确认响应列表
     */
    private void collectCraftResult(ErpCraftSyncResult result,
                                    ErpCraftSyncRespVO response,
                                    List<ErpCraftPendingRespVO> pendingItems) {
        if (result.duplicate()) {
            response.setDuplicateCount(response.getDuplicateCount() + 1);
        } else if (result.pending() != null
                && ErpCraftPendingStatusEnum.PENDING.getStatus()
                        .equals(result.pending().getStatus())) {
            response.setSuccessCount(response.getSuccessCount() + 1);
        } else {
            response.setFailureCount(response.getFailureCount() + 1);
        }
        if (result.pending() != null) {
            pendingItems.add(ErpSyncResponseConverter.toPendingResponse(result.pending()));
        }
    }

    /**
     * 判断业务异常是否匹配指定错误码。
     *
     * @param exception 业务异常
     * @param errorCode 预期错误码
     * @return 是否匹配
     */
    private boolean isErrorCode(ServiceException exception,
                                com.badminton.mes.common.core.ErrorCode errorCode) {
        return exception.getErrorCode() != null
                && errorCode.code().equals(exception.getErrorCode().code());
    }

    private String resolveTaskBusinessKey(ErpTaskDTO task) {
        return task != null && StringUtils.hasText(task.erpOrderNo())
                ? task.erpOrderNo().trim()
                : INVALID_BUSINESS_KEY;
    }

    private String resolveCraftBusinessKey(ErpCraftDTO craft) {
        if (craft == null
                || !StringUtils.hasText(craft.erpRoutingCode())
                || !StringUtils.hasText(craft.erpRoutingVersion())) {
            return INVALID_BUSINESS_KEY;
        }
        return craft.erpRoutingCode().trim() + ":" + craft.erpRoutingVersion().trim();
    }

    private List<ErpTaskDTO> filterTasks(
            List<ErpTaskDTO> tasks,
            String erpOrderNo,
            java.time.LocalDateTime startTime,
            java.time.LocalDateTime endTime) {
        String target = StringUtils.hasText(erpOrderNo) ? erpOrderNo.trim() : null;
        return tasks.stream()
                .filter(java.util.Objects::nonNull)
                .filter(task -> target == null || target.equals(task.erpOrderNo()))
                .filter(task -> startTime == null || (task.planStartTime() != null
                        && !task.planStartTime().isBefore(startTime)))
                .filter(task -> endTime == null || (task.planStartTime() != null
                        && !task.planStartTime().isAfter(endTime)))
                .toList();
    }

    private void validateTaskTimeRange(ErpTaskSyncReqVO reqVO) {
        if (reqVO.getStartTime() != null && reqVO.getEndTime() != null
                && reqVO.getEndTime().isBefore(reqVO.getStartTime())) {
            throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR,
                    "同步截止时间不能早于同步起始时间");
        }
    }

    private int normalizePageNo(int requestedPageNo, int pageSize, long total) {
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        return Math.min(requestedPageNo, totalPages);
    }

    private String resolveSourceSystem(String sourceSystem) {
        return StringUtils.hasText(sourceSystem)
                ? sourceSystem.trim().toUpperCase(Locale.ROOT)
                : ErpDataSource.DEFAULT_SOURCE_SYSTEM;
    }
}
