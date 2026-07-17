package com.badminton.mes.module.craft.service;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.craft.dal.entity.CraftProcessChangeLogEntity;
import com.badminton.mes.module.craft.dal.repository.CraftProcessChangeLogRepository;
import com.badminton.mes.module.craft.enums.CraftProcessChangeTypeEnum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

/**
 * 工序变更审计服务，统一完成业务快照序列化和日志落库。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Service
public class CraftProcessAuditService {

    private static final Logger logger = LoggerFactory.getLogger(CraftProcessAuditService.class);

    private final CraftProcessChangeLogRepository changeLogRepository;

    private final ObjectMapper objectMapper;

    /**
     * 构造器注入。
     *
     * @param changeLogRepository 工序变更日志 Repository
     * @param objectMapper        JSON 序列化器
     */
    public CraftProcessAuditService(CraftProcessChangeLogRepository changeLogRepository,
                                    ObjectMapper objectMapper) {
        this.changeLogRepository = changeLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 记录工序或子资源变更。
     *
     * @param processId   工序主键
     * @param changeType  变更类型
     * @param beforeValue 变更前业务快照对象，可为 null
     * @param afterValue  变更后业务快照对象，可为 null
     * @param reason      变更原因
     * @param operatorId  操作人主键
     */
    public void record(Long processId, CraftProcessChangeTypeEnum changeType,
                       Object beforeValue, Object afterValue, String reason, Long operatorId) {
        // 前后快照在写入日志实体前同步序列化，失败时让外层业务事务整体回滚。
        CraftProcessChangeLogEntity changeLog = new CraftProcessChangeLogEntity();
        changeLog.setProcessId(processId);
        changeLog.setChangeType(changeType.getType());
        changeLog.setBeforeSnapshot(serialize(beforeValue, processId));
        changeLog.setAfterSnapshot(serialize(afterValue, processId));
        changeLog.setChangeReason(reason);
        changeLog.setOperatorId(operatorId);
        changeLogRepository.save(changeLog);
    }

    /**
     * 序列化业务快照。
     *
     * @param value     快照对象，可为 null
     * @param processId 工序主键，用于异常日志定位
     * @return JSON 字符串；输入为 null 时返回 null
     */
    private String serialize(Object value, Long processId) {
        if (value == null) {
            // 创建操作没有前快照、删除操作没有后快照，数据库中按 null 明确表达缺省侧。
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (RuntimeException exception) {
            // 审计快照是业务变更的组成部分，序列化失败不能静默提交主数据。
            logger.error("[工序快照生成失败] processId: {}, errorMessage: {}",
                    processId, exception.getMessage(), exception);
            throw new ServiceException(GlobalErrorCodeConstants.SYSTEM_ERROR, "工序变更快照生成失败");
        }
    }
}
