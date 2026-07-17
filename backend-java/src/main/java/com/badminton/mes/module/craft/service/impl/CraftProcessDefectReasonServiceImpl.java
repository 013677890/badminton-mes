package com.badminton.mes.module.craft.service.impl;

import java.util.List;
import java.util.Locale;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.constants.CraftErrorCodeConstants;
import com.badminton.mes.module.craft.controller.vo.CraftProcessDefectReasonRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessDefectReasonSaveReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessDefectReasonUpdateReqVO;
import com.badminton.mes.module.craft.convert.CraftProcessConvert;
import com.badminton.mes.module.craft.dal.entity.CraftProcessDefectReasonEntity;
import com.badminton.mes.module.craft.dal.redis.CraftCache;
import com.badminton.mes.module.craft.dal.repository.CraftProcessDefectReasonRepository;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.craft.enums.CraftProcessChangeTypeEnum;
import com.badminton.mes.module.craft.service.CraftProcessAuditService;
import com.badminton.mes.module.craft.service.CraftProcessDefectReasonService;
import com.badminton.mes.module.craft.service.support.CraftPersistenceExceptionTranslator;
import com.badminton.mes.module.craft.service.support.CraftVersionValidator;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 工序不良原因关联 Service 实现。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Service
public class CraftProcessDefectReasonServiceImpl implements CraftProcessDefectReasonService {

    private static final int REASON_CODE_MAX_LENGTH = 32;

    private static final String ACTIVE_REASON_CODE_CONSTRAINT = "uk_process_active_reason_code";

    private final CraftProcessRepository processRepository;

    private final CraftProcessDefectReasonRepository defectReasonRepository;

    private final CraftProcessAuditService auditService;

    private final CraftCache craftCache;

    /**
     * 构造器注入。
     *
     * @param processRepository      工序 Repository
     * @param defectReasonRepository 工序不良原因 Repository
     * @param auditService           工序变更审计服务
     * @param craftCache             工艺 Redis 缓存
     */
    public CraftProcessDefectReasonServiceImpl(CraftProcessRepository processRepository,
                                               CraftProcessDefectReasonRepository defectReasonRepository,
                                               CraftProcessAuditService auditService,
                                               CraftCache craftCache) {
        this.processRepository = processRepository;
        this.defectReasonRepository = defectReasonRepository;
        this.auditService = auditService;
        this.craftCache = craftCache;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProcessDefectReason(Long processId,
                                          CraftProcessDefectReasonSaveReqVO reqVO) {
        // 先确认父工序有效，再按规范化后的编码检查同一工序内唯一性。
        requireProcess(processId);
        normalizeRequest(reqVO);
        validateNormalizedCodeLength(reqVO.getReasonCode());
        validateReasonCode(processId, reqVO.getReasonCode(), null);

        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        CraftProcessDefectReasonEntity reason = new CraftProcessDefectReasonEntity();
        reason.setProcessId(processId);
        copyToEntity(reqVO, reason);
        reason.setCreateBy(operatorId);
        reason.setUpdateBy(operatorId);
        // 立即落库取得关联主键，并让数据库唯一约束在审计记录写入前完成校验。
        saveReason(reason);
        auditService.record(processId, CraftProcessChangeTypeEnum.DEFECT_REASON_BINDING,
                null, CraftProcessConvert.toDefectReasonRespVO(reason),
                defaultReason(reqVO.getChangeReason(), "新增工序不良原因"), operatorId);
        craftCache.evictProcessDefectReasonsAfterCommit(processId);
        return reason.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProcessDefectReason(Long processId, Long reasonId,
                                          CraftProcessDefectReasonUpdateReqVO reqVO) {
        requireProcess(processId);
        // 查询条件同时包含父工序 id，防止通过其他工序的关联主键越界修改。
        CraftProcessDefectReasonEntity reason = requireReason(processId, reasonId);
        CraftVersionValidator.validate(reason.getVersion(), reqVO.getVersion(),
                CraftErrorCodeConstants.PROCESS_DEFECT_REASON_CONCURRENT_MODIFICATION);
        normalizeRequest(reqVO);
        validateNormalizedCodeLength(reqVO.getReasonCode());
        validateReasonCode(processId, reqVO.getReasonCode(), reasonId);

        // 在覆盖业务字段前保留响应形态快照，便于审计日志直接展示前后变化。
        CraftProcessDefectReasonRespVO beforeSnapshot =
                CraftProcessConvert.toDefectReasonRespVO(reason);
        copyToEntity(reqVO, reason);
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        reason.setUpdateBy(operatorId);
        saveReason(reason);
        auditService.record(processId, CraftProcessChangeTypeEnum.DEFECT_REASON_BINDING,
                beforeSnapshot, CraftProcessConvert.toDefectReasonRespVO(reason),
                defaultReason(reqVO.getChangeReason(), "修改工序不良原因"), operatorId);
        craftCache.evictProcessDefectReasonsAfterCommit(processId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProcessDefectReason(Long processId, Long reasonId, Integer expectedVersion) {
        requireProcess(processId);
        CraftProcessDefectReasonEntity reason = requireReason(processId, reasonId);
        CraftVersionValidator.validate(reason.getVersion(), expectedVersion,
                CraftErrorCodeConstants.PROCESS_DEFECT_REASON_CONCURRENT_MODIFICATION);

        // 采用软删除保留历史不良原因，并在审计中记录删除前完整内容。
        CraftProcessDefectReasonRespVO beforeSnapshot =
                CraftProcessConvert.toDefectReasonRespVO(reason);
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        reason.setDeleted(true);
        reason.setUpdateBy(operatorId);
        saveReason(reason);
        auditService.record(processId, CraftProcessChangeTypeEnum.DEFECT_REASON_BINDING,
                beforeSnapshot, null, "删除工序不良原因", operatorId);
        craftCache.evictProcessDefectReasonsAfterCommit(processId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CraftProcessDefectReasonRespVO> getProcessDefectReasons(Long processId) {
        // 列表按工序维度使用旁路缓存，缓存未命中时才校验父工序并访问数据库。
        List<CraftProcessDefectReasonRespVO> cached =
                craftCache.getProcessDefectReasons(processId).orElse(null);
        if (cached != null) {
            return cached;
        }

        requireProcess(processId);
        List<CraftProcessDefectReasonRespVO> result = CraftProcessConvert.toDefectReasonRespVOList(
                defectReasonRepository.findByProcessIdAndDeletedFalseOrderByIdAsc(processId));
        // 空列表也写入缓存，避免没有配置不良原因的工序被反复回源查询。
        craftCache.putProcessDefectReasons(processId, result);
        return result;
    }

    /**
     * 校验工序存在且未删除。
     *
     * @param processId 工序主键
     */
    private void requireProcess(Long processId) {
        if (processRepository.findByIdAndDeletedFalse(processId).isEmpty()) {
            throw new ServiceException(CraftErrorCodeConstants.PROCESS_NOT_EXISTS);
        }
    }

    /**
     * 查询工序下未删除的不良原因。
     *
     * @param processId 工序主键
     * @param reasonId  不良原因关联主键
     * @return 不良原因实体
     */
    private CraftProcessDefectReasonEntity requireReason(Long processId, Long reasonId) {
        return defectReasonRepository.findByIdAndProcessIdAndDeletedFalse(reasonId, processId)
                .orElseThrow(() -> new ServiceException(
                        CraftErrorCodeConstants.PROCESS_DEFECT_REASON_NOT_EXISTS));
    }

    /**
     * 校验同一工序下不良原因编码唯一。
     *
     * @param processId 工序主键
     * @param reasonCode 不良原因编码
     * @param excludeId 排除的关联主键，创建时为 null
     */
    private void validateReasonCode(Long processId, String reasonCode, Long excludeId) {
        // 应用层预检用于友好提示；并发创建仍由数据库活动编码唯一约束兜底。
        boolean exists = excludeId == null
                ? defectReasonRepository.existsByProcessIdAndReasonCodeAndDeletedFalse(
                        processId, reasonCode)
                : defectReasonRepository.existsByProcessIdAndReasonCodeAndIdNotAndDeletedFalse(
                        processId, reasonCode, excludeId);
        if (exists) {
            throw new ServiceException(
                    CraftErrorCodeConstants.PROCESS_DEFECT_REASON_CODE_DUPLICATE);
        }
    }

    /**
     * 保存不良原因并转换可识别的并发或唯一约束异常。
     *
     * @param reason 不良原因实体
     */
    private void saveReason(CraftProcessDefectReasonEntity reason) {
        try {
            // flush 将唯一约束和乐观锁异常限制在当前方法内，便于转换为稳定业务错误。
            defectReasonRepository.saveAndFlush(reason);
        } catch (DataIntegrityViolationException exception) {
            CraftPersistenceExceptionTranslator.translateUniqueConstraint(exception,
                    ACTIVE_REASON_CODE_CONSTRAINT,
                    CraftErrorCodeConstants.PROCESS_DEFECT_REASON_CODE_DUPLICATE);
        } catch (OptimisticLockingFailureException exception) {
            throw new ServiceException(
                    CraftErrorCodeConstants.PROCESS_DEFECT_REASON_CONCURRENT_MODIFICATION);
        }
    }

    /**
     * 规范化不良原因保存请求。
     *
     * @param reqVO 保存请求
     */
    private void normalizeRequest(CraftProcessDefectReasonSaveReqVO reqVO) {
        reqVO.setReasonCode(reqVO.getReasonCode().trim().toUpperCase(Locale.ROOT));
        reqVO.setReasonName(reqVO.getReasonName().trim());
        reqVO.setChangeReason(trimToNull(reqVO.getChangeReason()));
    }

    /**
     * 在规范化后再次保护数据库编码长度。
     *
     * @param reasonCode 已规范化不良原因编码
     */
    private void validateNormalizedCodeLength(String reasonCode) {
        if (reasonCode.length() > REASON_CODE_MAX_LENGTH) {
            throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR,
                    "规范化后的不良原因编码长度不能超过 32");
        }
    }

    /**
     * 复制不良原因业务字段。
     *
     * @param reqVO 保存请求
     * @param reason 不良原因实体
     */
    private void copyToEntity(CraftProcessDefectReasonSaveReqVO reqVO,
                              CraftProcessDefectReasonEntity reason) {
        reason.setReasonCode(reqVO.getReasonCode());
        reason.setReasonName(reqVO.getReasonName());
        reason.setStatus(reqVO.getStatus());
    }

    /**
     * 字符串去空格，空白值转 null。
     *
     * @param value 原始字符串
     * @return 规范化字符串
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 请求未提供原因时返回默认原因。
     *
     * @param reason        请求原因
     * @param defaultReason 默认原因
     * @return 最终原因
     */
    private String defaultReason(String reason, String defaultReason) {
        return StringUtils.hasText(reason) ? reason : defaultReason;
    }
}
