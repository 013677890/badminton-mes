package com.badminton.mes.module.integration.service;

import com.badminton.mes.common.core.ErrorCode;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.integration.constants.IntegrationErrorCodeConstants;
import com.badminton.mes.module.integration.dal.entity.IntegrationWriteLogEntity;
import com.badminton.mes.module.integration.dal.repository.IntegrationWriteLogRepository;
import com.badminton.mes.module.integration.enums.IntegrationInterfaceTypeEnum;
import com.badminton.mes.module.integration.enums.IntegrationWriteStatusEnum;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

/**
 * 外部接口请求快照与写入结果审计服务。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@Service
public class IntegrationAuditService {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 512;

    private final IntegrationWriteLogRepository writeLogRepository;

    private final ObjectMapper objectMapper;

    /**
     * 构造审计服务。
     *
     * @param writeLogRepository 写入日志 Repository
     * @param objectMapper       JSON 序列化器
     */
    public IntegrationAuditService(IntegrationWriteLogRepository writeLogRepository,
                                   ObjectMapper objectMapper) {
        this.writeLogRepository = writeLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 将已通过请求体校验的 DTO 序列化为日志快照。
     *
     * @param request 请求 DTO
     * @return JSON 快照
     */
    public String serializeRequest(Object request) {
        return objectMapper.writeValueAsString(request);
    }

    /**
     * 在当前业务事务中记录成功或重复结果，使主数据与日志原子提交。
     *
     * @param interfaceType 接口类型
     * @param sourceSystem  来源系统
     * @param businessKey   来源业务键
     * @param snapshot      请求快照
     * @param status        成功或重复状态
     * @param resultId      MES 业务主键
     * @param resultNo      MES 业务编号
     * @return 日志主键
     */
    @Transactional(rollbackFor = Exception.class)
    public Long recordResult(IntegrationInterfaceTypeEnum interfaceType,
                             String sourceSystem,
                             String businessKey,
                             String snapshot,
                             IntegrationWriteStatusEnum status,
                             Long resultId,
                             String resultNo) {
        IntegrationWriteLogEntity log = buildLog(
                interfaceType, sourceSystem, businessKey, snapshot, status);
        log.setResultId(resultId);
        log.setResultNo(resultNo);
        return writeLogRepository.saveAndFlush(log).getId();
    }

    /**
     * 在独立事务中记录并发竞争产生的重复结果，不写失败错误信息。
     *
     * @param interfaceType 接口类型
     * @param sourceSystem  来源系统
     * @param businessKey   来源业务键
     * @param snapshot      请求快照
     * @param resultId      已存在业务主键
     * @param resultNo      已存在业务编号
     * @return 日志主键
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Long recordDuplicate(IntegrationInterfaceTypeEnum interfaceType,
                                String sourceSystem,
                                String businessKey,
                                String snapshot,
                                Long resultId,
                                String resultNo) {
        IntegrationWriteLogEntity log = buildLog(
                interfaceType, sourceSystem, businessKey, snapshot,
                IntegrationWriteStatusEnum.DUPLICATE);
        log.setResultId(resultId);
        log.setResultNo(resultNo);
        return writeLogRepository.saveAndFlush(log).getId();
    }

    /**
     * 在独立事务中记录已回滚业务命令的失败结果。
     *
     * @param interfaceType 接口类型
     * @param sourceSystem  来源系统
     * @param businessKey   来源业务键
     * @param snapshot      请求快照
     * @param resultId      已存在业务主键
     * @param resultNo      已存在业务编号
     * @param errorCode     业务错误码
     * @param errorMessage  失败原因
     * @return 日志主键
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Long recordFailure(IntegrationInterfaceTypeEnum interfaceType,
                              String sourceSystem,
                              String businessKey,
                              String snapshot,
                              Long resultId,
                              String resultNo,
                              ErrorCode errorCode,
                              String errorMessage) {
        IntegrationWriteLogEntity log = buildLog(
                interfaceType, sourceSystem, businessKey, snapshot,
                IntegrationWriteStatusEnum.FAILED);
        log.setResultId(resultId);
        log.setResultNo(resultNo);
        log.setErrorCode(errorCode.code());
        log.setErrorMessage(truncate(errorMessage));
        return writeLogRepository.saveAndFlush(log).getId();
    }

    /**
     * 在当前业务事务中记录已保留失败业务数据的审计结果。
     *
     * <p>适用于失败数据本身也需要落库的场景，使失败业务记录与日志原子提交；
     * 已回滚命令仍使用 {@link #recordFailure} 在独立事务中记录。
     *
     * @param interfaceType 接口类型
     * @param sourceSystem  来源系统
     * @param businessKey   来源业务键
     * @param snapshot      请求快照
     * @param resultId      失败业务记录主键
     * @param resultNo      失败业务记录编号
     * @param errorCode     业务错误码
     * @param errorMessage  失败原因
     * @return 日志主键
     */
    @Transactional(rollbackFor = Exception.class)
    public Long recordFailureInCurrentTransaction(
            IntegrationInterfaceTypeEnum interfaceType,
            String sourceSystem,
            String businessKey,
            String snapshot,
            Long resultId,
            String resultNo,
            ErrorCode errorCode,
            String errorMessage) {
        IntegrationWriteLogEntity log = buildLog(
                interfaceType, sourceSystem, businessKey, snapshot,
                IntegrationWriteStatusEnum.FAILED);
        log.setResultId(resultId);
        log.setResultNo(resultNo);
        log.setErrorCode(errorCode.code());
        log.setErrorMessage(truncate(errorMessage));
        return writeLogRepository.saveAndFlush(log).getId();
    }

    /**
     * 将设备计数原失败日志原子替换为最后一次重试结果。
     *
     * <p>设备计数日志的来源幂等键具有唯一约束，原幂等键修正重试时不能追加新日志，
     * 因此保留日志主键并更新请求快照和最终业务结果。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long replaceFailureResult(Long logId,
                                     String snapshot,
                                     IntegrationWriteStatusEnum status,
                                     Long resultId,
                                     String resultNo,
                                     ErrorCode errorCode) {
        IntegrationWriteLogEntity log = writeLogRepository.findById(logId)
                .orElseThrow(() -> new ServiceException(
                        IntegrationErrorCodeConstants.WRITE_CONFLICT));
        if (!IntegrationWriteStatusEnum.FAILED.getStatus().equals(log.getWriteStatus())) {
            throw new ServiceException(IntegrationErrorCodeConstants.WRITE_CONFLICT);
        }
        log.setRequestSnapshot(snapshot);
        log.setWriteStatus(status.getStatus());
        log.setResultId(resultId);
        log.setResultNo(resultNo);
        log.setErrorCode(errorCode == null ? null : errorCode.code());
        log.setErrorMessage(errorCode == null ? null : truncate(errorCode.message()));
        return writeLogRepository.saveAndFlush(log).getId();
    }

    /**
     * 构造日志公共字段。
     *
     * @param interfaceType 接口类型
     * @param sourceSystem  来源系统
     * @param businessKey   来源业务键
     * @param snapshot      请求快照
     * @param status        写入状态
     * @return 日志实体
     */
    private IntegrationWriteLogEntity buildLog(IntegrationInterfaceTypeEnum interfaceType,
                                                String sourceSystem,
                                                String businessKey,
                                                String snapshot,
                                                IntegrationWriteStatusEnum status) {
        IntegrationWriteLogEntity log = new IntegrationWriteLogEntity();
        log.setInterfaceType(interfaceType.getValue());
        log.setSourceSystem(sourceSystem);
        log.setBusinessKey(businessKey);
        log.setRequestSnapshot(snapshot);
        log.setWriteStatus(status.getStatus());
        log.setCreateBy(SecurityContextHolder.getRequiredLoginUserId());
        return log;
    }

    /**
     * 将失败原因限制到数据库列长度，避免审计写入反向破坏错误响应。
     *
     * @param message 原始失败原因
     * @return 截断后的失败原因
     */
    private String truncate(String message) {
        if (message == null || message.length() <= ERROR_MESSAGE_MAX_LENGTH) {
            return message;
        }
        return message.substring(0, ERROR_MESSAGE_MAX_LENGTH);
    }
}
