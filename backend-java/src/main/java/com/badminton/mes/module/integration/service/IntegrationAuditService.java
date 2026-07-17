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
 * <p>成功、普通重复结果加入调用方事务，与业务数据原子提交；业务事务已经回滚后的失败或并发
 * 重复结果使用独立事务保存，确保失败事实不会随主事务一起消失。设备计数修正重试因幂等键具有
 * 唯一约束，会原位替换失败日志，而不是插入第二条相同业务键记录。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@Service
public class IntegrationAuditService {

    /** 数据库错误信息列最大长度，超长异常文本在写审计前截断。 */
    private static final int ERROR_MESSAGE_MAX_LENGTH = 512;

    /** 接口写入日志仓储，承担审计记录新增、失败记录读取和原位更新。 */
    private final IntegrationWriteLogRepository writeLogRepository;

    /** 请求对象 JSON 序列化器，用于固化外部调用的原始字段快照。 */
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
        // 序列化异常直接向上抛出，防止缺失请求快照的接口命令继续执行并留下不可审计结果。
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
        // 保存 MES 结果定位信息，后续重复请求可直接复用既有业务结果。
        log.setResultId(resultId);
        log.setResultNo(resultNo);
        // 立即刷新使日志约束异常在业务事务提交前暴露，并与主数据一起回滚。
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
        // REQUIRES_NEW 与已经标记回滚的原事务隔离，保证并发竞争结果仍能独立留痕。
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
        // 失败日志必须在新事务落库，否则会跟随失败的业务命令一同回滚而失去审计价值。
        IntegrationWriteLogEntity log = buildLog(
                interfaceType, sourceSystem, businessKey, snapshot,
                IntegrationWriteStatusEnum.FAILED);
        log.setResultId(resultId);
        log.setResultNo(resultNo);
        log.setErrorCode(errorCode.code());
        // 先按列长度截断，避免原始异常过长导致“记录失败日志”本身再次失败。
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
        // 该场景会保留失败业务实体，因此日志必须加入当前事务，确保二者同时成功或同时回滚。
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
        // 按日志主键读取原记录，避免用来源幂等键误更新其他接口类型的审计数据。
        IntegrationWriteLogEntity log = writeLogRepository.findById(logId)
                .orElseThrow(() -> new ServiceException(
                        IntegrationErrorCodeConstants.WRITE_CONFLICT));
        if (!IntegrationWriteStatusEnum.FAILED.getStatus().equals(log.getWriteStatus())) {
            // 只有失败日志允许被修正重试覆盖，成功或重复结果必须保持不可变。
            throw new ServiceException(IntegrationErrorCodeConstants.WRITE_CONFLICT);
        }
        // 保留日志主键和幂等键，只替换本次请求快照、最终状态及业务结果定位信息。
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
        // 接口类型、来源系统和业务键共同描述一次跨系统写入的审计身份。
        log.setInterfaceType(interfaceType.getValue());
        log.setSourceSystem(sourceSystem);
        log.setBusinessKey(businessKey);
        log.setRequestSnapshot(snapshot);
        log.setWriteStatus(status.getStatus());
        // 创建人取当前认证上下文，禁止由外部请求伪造审计操作者。
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
