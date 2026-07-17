package com.badminton.mes.module.integration.service.impl;

import java.util.Locale;
import java.util.Optional;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.integration.constants.IntegrationErrorCodeConstants;
import com.badminton.mes.module.integration.controller.vo.ExternalDispatchOrderWriteReqVO;
import com.badminton.mes.module.integration.controller.vo.DeviceCountWriteReqVO;
import com.badminton.mes.module.integration.controller.vo.ExternalWorkOrderWriteReqVO;
import com.badminton.mes.module.integration.controller.vo.IntegrationWriteLogPageReqVO;
import com.badminton.mes.module.integration.controller.vo.IntegrationWriteLogRespVO;
import com.badminton.mes.module.integration.controller.vo.IntegrationWriteResultRespVO;
import com.badminton.mes.module.integration.controller.vo.UnitWriteReqVO;
import com.badminton.mes.module.integration.dal.entity.IntegrationWriteLogEntity;
import com.badminton.mes.module.integration.dal.repository.IntegrationWriteLogRepository;
import com.badminton.mes.module.integration.dal.repository.IntegrationWriteLogSpecifications;
import com.badminton.mes.module.integration.enums.IntegrationInterfaceTypeEnum;
import com.badminton.mes.module.integration.enums.IntegrationWriteStatusEnum;
import com.badminton.mes.module.integration.service.IntegrationAuditService;
import com.badminton.mes.module.integration.service.DeviceCountWriteCommandService;
import com.badminton.mes.module.integration.service.IntegrationDispatchWriteCommandService;
import com.badminton.mes.module.integration.service.IntegrationService;
import com.badminton.mes.module.integration.service.IntegrationWriteCommandService;
import com.badminton.mes.module.integration.service.dto.IntegrationCommandResult;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 外部标准写入接口门面实现。
 *
 * <p>成功主数据与日志由命令事务原子提交；命令回滚后的失败日志使用独立事务记录。
 * 业务校验失败作为稳定的写入结果返回，便于外部系统按日志主键追踪。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@Service
public class IntegrationServiceImpl implements IntegrationService {

    /** 单位和外部工单事务命令服务，负责业务写入与成功日志原子提交。 */
    private final IntegrationWriteCommandService commandService;

    /** 外部派工事务命令服务，通过生产模块领域服务创建任务单。 */
    private final IntegrationDispatchWriteCommandService dispatchCommandService;

    /** 设备累计计数命令服务，负责异常池、增量计算和自动报工。 */
    private final DeviceCountWriteCommandService deviceCountCommandService;

    /** 审计服务，用于在命令事务回滚后以独立事务记录失败或并发重复结果。 */
    private final IntegrationAuditService auditService;

    /** 写入日志仓储，用于接口日志分页和幂等结果补充查询。 */
    private final IntegrationWriteLogRepository writeLogRepository;

    /**
     * 构造外部接口门面。
     *
     * @param commandService         单位与生产工单写入命令服务
     * @param dispatchCommandService 生产任务单写入命令服务
     * @param deviceCountCommandService 设备累计计数写入命令服务
     * @param auditService           接口审计服务
     * @param writeLogRepository     写入日志 Repository
     */
    public IntegrationServiceImpl(IntegrationWriteCommandService commandService,
                                  IntegrationDispatchWriteCommandService dispatchCommandService,
                                  DeviceCountWriteCommandService deviceCountCommandService,
                                  IntegrationAuditService auditService,
                                  IntegrationWriteLogRepository writeLogRepository) {
        this.commandService = commandService;
        this.dispatchCommandService = dispatchCommandService;
        this.deviceCountCommandService = deviceCountCommandService;
        this.auditService = auditService;
        this.writeLogRepository = writeLogRepository;
    }

    @Override
    public IntegrationWriteResultRespVO writeUnit(UnitWriteReqVO reqVO) {
        // 在进入事务命令前固定请求快照和规范化业务键，失败审计仍可复用同一份内容。
        String snapshot = auditService.serializeRequest(reqVO);
        String sourceSystem = normalizeCode(reqVO.getSourceSystem());
        String unitCode = normalizeCode(reqVO.getUnitCode());
        try {
            return toSuccess(commandService.writeUnit(reqVO, snapshot));
        } catch (ServiceException exception) {
            boolean duplicate = IntegrationErrorCodeConstants.UNIT_CODE_DUPLICATE
                    .equals(exception.getErrorCode());
            if (duplicate) {
                // 单位契约是 upsert；并发首次新增的落败事务应重试更新，而不是返回普通重复。
                return retryConcurrentUnitUpsert(reqVO, snapshot, sourceSystem, unitCode);
            }
            return recordFailure(
                    IntegrationInterfaceTypeEnum.UNIT_WRITE,
                    sourceSystem,
                    unitCode,
                    snapshot,
                    exception,
                    null,
                    null);
        }
    }

    @Override
    public IntegrationWriteResultRespVO writeWorkOrder(ExternalWorkOrderWriteReqVO reqVO) {
        // 来源系统与外部工单号是后续成功、失败和并发回查共同使用的审计业务键。
        String snapshot = auditService.serializeRequest(reqVO);
        String sourceSystem = normalizeCode(reqVO.getSourceSystem());
        String externalNo = reqVO.getExternalWorkOrderNo().trim();
        try {
            return toSuccess(commandService.writeWorkOrder(reqVO, snapshot));
        } catch (ServiceException exception) {
            boolean duplicate = IntegrationErrorCodeConstants.EXTERNAL_WORK_ORDER_DUPLICATE
                    .equals(exception.getErrorCode());
            // 唯一键竞争说明另一事务可能已提交，回查其工单即可恢复稳定的幂等响应。
            Optional<WorkOrderEntity> existing = duplicate
                    ? commandService.findExternalWorkOrder(sourceSystem, externalNo)
                    : Optional.empty();
            if (duplicate) {
                if (existing.isEmpty()) {
                    // 约束冲突却无法回查获胜记录时，不伪造重复结果，按数据库写冲突留痕。
                    return recordFailure(
                            IntegrationInterfaceTypeEnum.WORK_ORDER_WRITE,
                            sourceSystem,
                            externalNo,
                            snapshot,
                            new ServiceException(IntegrationErrorCodeConstants.WRITE_CONFLICT),
                            null,
                            null);
                }
                // 获胜工单可见后，以独立事务记录本次并发重复请求。
                return recordDuplicate(
                        IntegrationInterfaceTypeEnum.WORK_ORDER_WRITE,
                        sourceSystem,
                        externalNo,
                        snapshot,
                        existing.map(WorkOrderEntity::getId).orElse(null),
                        existing.map(WorkOrderEntity::getWorkOrderNo).orElse(null));
            }
            return recordFailure(
                    IntegrationInterfaceTypeEnum.WORK_ORDER_WRITE,
                    sourceSystem,
                    externalNo,
                    snapshot,
                    exception,
                    existing.map(WorkOrderEntity::getId).orElse(null),
                    existing.map(WorkOrderEntity::getWorkOrderNo).orElse(null));
        }
    }

    @Override
    public IntegrationWriteResultRespVO writeDispatchOrder(ExternalDispatchOrderWriteReqVO reqVO) {
        // 派工单没有外部来源字段，因此失败时从审计日志回查可能已存在的业务结果。
        String snapshot = auditService.serializeRequest(reqVO);
        String sourceSystem = normalizeCode(reqVO.getSourceSystem());
        String externalNo = reqVO.getExternalDispatchOrderNo().trim();
        try {
            return toSuccess(dispatchCommandService.writeDispatchOrder(reqVO, snapshot));
        } catch (ServiceException exception) {
            Optional<IntegrationWriteLogEntity> existing =
                    dispatchCommandService.findExternalDispatchLog(sourceSystem, externalNo);
            return recordFailure(
                    IntegrationInterfaceTypeEnum.DISPATCH_ORDER_WRITE,
                    sourceSystem,
                    externalNo,
                    snapshot,
                    exception,
                    existing.map(IntegrationWriteLogEntity::getResultId).orElse(null),
                    existing.map(IntegrationWriteLogEntity::getResultNo).orElse(null));
        }
    }

    @Override
    public IntegrationWriteResultRespVO writeDeviceCount(DeviceCountWriteReqVO reqVO) {
        // 先查日志可在进入加锁和累计校验前快速返回已经完成的幂等结果。
        String snapshot = auditService.serializeRequest(reqVO);
        String sourceSystem = normalizeCode(reqVO.getSourceSystem());
        String externalKey = normalizeCode(reqVO.getExternalKey());
        Optional<IntegrationWriteLogEntity> processedLog =
                deviceCountCommandService.findProcessedLog(sourceSystem, externalKey);
        if (processedLog.isPresent()) {
            // 原结果即使是 FAILED 也必须沿用其错误码，防止相同幂等键绕过异常池重复落库。
            return toDeviceCountDuplicate(processedLog.get());
        }
        try {
            return deviceCountCommandService.writeDeviceCount(reqVO, snapshot);
        } catch (ServiceException exception) {
            boolean duplicate = IntegrationErrorCodeConstants.DEVICE_COUNT_DUPLICATE
                    .equals(exception.getErrorCode());
            Optional<IntegrationWriteLogEntity> existing = duplicate
                    ? deviceCountCommandService.findProcessedLog(sourceSystem, externalKey)
                    : Optional.empty();
            if (existing.isPresent()) {
                // 并发唯一键落败后回查获胜日志，将数据库竞争转换为稳定 DUPLICATE 响应。
                return toDeviceCountDuplicate(existing.get());
            }
            return recordFailure(
                    IntegrationInterfaceTypeEnum.DEVICE_COUNT_WRITE,
                    sourceSystem,
                    externalKey,
                    snapshot,
                    duplicate
                            ? new ServiceException(IntegrationErrorCodeConstants.WRITE_CONFLICT)
                            : exception,
                    null,
                    null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<IntegrationWriteLogRespVO> getWriteLogPage(
            IntegrationWriteLogPageReqVO reqVO) {
        // 动态规格统一拼装接口类型、来源、状态和时间等可选数据库条件。
        Specification<IntegrationWriteLogEntity> specification =
                IntegrationWriteLogSpecifications.page(reqVO);
        // 空结果跳过列表查询，避免无意义的第二次数据库往返。
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
        return PageResult.of(page.getContent().stream().map(this::toRespVO).toList(),
                total, pageNo, pageSize);
    }

    /**
     * 首次并发新增单位发生唯一键竞争后，重新开启事务执行既有单位更新。
     *
     * <p>单位接口契约是 upsert，不能因为两个首次请求的到达顺序不同而把落败请求
     * 降级为重复。首次事务已回滚，第二次调用可安全锁定获胜事务插入的单位行。
     *
     * @param reqVO        单位写入请求
     * @param snapshot     请求快照
     * @param sourceSystem 来源系统
     * @param unitCode     规范化单位编码
     * @return 最终写入结果
     */
    private IntegrationWriteResultRespVO retryConcurrentUnitUpsert(
            UnitWriteReqVO reqVO,
            String snapshot,
            String sourceSystem,
            String unitCode) {
        try {
            // 原事务已经回滚，再次进入代理事务后可锁定获胜事务插入的单位行并完成真正 upsert。
            return toSuccess(commandService.writeUnit(reqVO, snapshot));
        } catch (ServiceException retryException) {
            return recordFailure(
                    IntegrationInterfaceTypeEnum.UNIT_WRITE,
                    sourceSystem,
                    unitCode,
                    snapshot,
                    retryException,
                    null,
                    null);
        }
    }

    /**
     * 将命令结果转换为成功或重复响应。
     *
     * @param commandResult 命令结果
     * @return 写入响应
     */
    private IntegrationWriteResultRespVO toSuccess(IntegrationCommandResult commandResult) {
        IntegrationWriteResultRespVO response = new IntegrationWriteResultRespVO();
        response.setLogId(commandResult.logId());
        response.setBusinessId(commandResult.businessId());
        response.setBusinessNo(commandResult.businessNo());
        if (commandResult.duplicate()) {
            response.setStatus(IntegrationWriteStatusEnum.DUPLICATE.getCode());
            response.setMessage("重复请求，未生成新数据");
        } else {
            response.setStatus(IntegrationWriteStatusEnum.SUCCESS.getCode());
            response.setMessage("写入成功");
        }
        return response;
    }

    /**
     * 记录失败结果并构造稳定响应。
     *
     * @param interfaceType 接口类型
     * @param sourceSystem  来源系统
     * @param businessKey   来源业务键
     * @param snapshot      请求快照
     * @param exception     业务异常
     * @param resultId      已存在业务主键
     * @param resultNo      已存在业务编号
     * @return 写入响应
     */
    private IntegrationWriteResultRespVO recordFailure(
            IntegrationInterfaceTypeEnum interfaceType,
            String sourceSystem,
            String businessKey,
            String snapshot,
            ServiceException exception,
            Long resultId,
            String resultNo) {
        // 命令事务已经退出，recordFailure 使用 REQUIRES_NEW 保证失败日志独立提交。
        Long logId = auditService.recordFailure(
                interfaceType, sourceSystem, businessKey, snapshot,
                resultId, resultNo, exception.getErrorCode(), exception.getMessage());
        IntegrationWriteResultRespVO response = new IntegrationWriteResultRespVO();
        response.setLogId(logId);
        response.setStatus(IntegrationWriteStatusEnum.FAILED.getCode());
        response.setBusinessId(resultId);
        response.setBusinessNo(resultNo);
        response.setErrorCode(exception.getErrorCode().code());
        response.setMessage(exception.getMessage());
        return response;
    }

    /**
     * 记录并发重复结果并构造与前置查重一致的响应。
     *
     * @param interfaceType 接口类型
     * @param sourceSystem  来源系统
     * @param businessKey   来源业务键
     * @param snapshot      请求快照
     * @param resultId      已存在业务主键
     * @param resultNo      已存在业务编号
     * @return 不含失败错误码的重复响应
     */
    private IntegrationWriteResultRespVO recordDuplicate(
            IntegrationInterfaceTypeEnum interfaceType,
            String sourceSystem,
            String businessKey,
            String snapshot,
            Long resultId,
            String resultNo) {
        // 并发竞争事务已回滚，重复日志同样使用独立事务保存。
        Long logId = auditService.recordDuplicate(
                interfaceType, sourceSystem, businessKey, snapshot, resultId, resultNo);
        IntegrationWriteResultRespVO response = new IntegrationWriteResultRespVO();
        response.setLogId(logId);
        response.setStatus(IntegrationWriteStatusEnum.DUPLICATE.getCode());
        response.setBusinessId(resultId);
        response.setBusinessNo(resultNo);
        response.setMessage("重复请求，未生成新数据");
        return response;
    }

    /**
     * 将设备计数唯一键竞争的获胜日志转换为稳定重复响应。
     *
     * @param log 原处理日志
     * @return 重复写入响应
     */
    private IntegrationWriteResultRespVO toDeviceCountDuplicate(IntegrationWriteLogEntity log) {
        IntegrationWriteResultRespVO response = new IntegrationWriteResultRespVO();
        response.setLogId(log.getId());
        response.setStatus(IntegrationWriteStatusEnum.DUPLICATE.getCode());
        response.setBusinessId(log.getResultId());
        response.setBusinessNo(log.getResultNo());
        response.setErrorCode(log.getErrorCode());
        response.setMessage("重复请求，沿用原设备计数处理结果");
        return response;
    }

    /**
     * 转换日志响应。
     *
     * @param entity 日志实体
     * @return 日志响应
     */
    private IntegrationWriteLogRespVO toRespVO(IntegrationWriteLogEntity entity) {
        IntegrationWriteLogRespVO response = new IntegrationWriteLogRespVO();
        response.setId(entity.getId());
        response.setInterfaceType(entity.getInterfaceType());
        response.setSourceSystem(entity.getSourceSystem());
        response.setBusinessKey(entity.getBusinessKey());
        response.setRequestSnapshot(entity.getRequestSnapshot());
        response.setWriteStatus(entity.getWriteStatus());
        response.setResultId(entity.getResultId());
        response.setResultNo(entity.getResultNo());
        response.setErrorCode(entity.getErrorCode());
        response.setErrorMessage(entity.getErrorMessage());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());
        return response;
    }

    /**
     * 修正超出总页数的请求页码。
     *
     * @param requestedPageNo 请求页码
     * @param pageSize        每页条数
     * @param total           总记录数
     * @return 实际页码
     */
    private int normalizePageNo(int requestedPageNo, int pageSize, long total) {
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        return Math.min(requestedPageNo, totalPages);
    }

    /**
     * 规范化 ASCII 编码。
     *
     * @param value 原始编码
     * @return 去空格大写编码
     */
    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
