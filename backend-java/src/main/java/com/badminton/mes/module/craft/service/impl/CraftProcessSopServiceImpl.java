package com.badminton.mes.module.craft.service.impl;

import java.util.List;
import java.util.Locale;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.constants.CraftErrorCodeConstants;
import com.badminton.mes.module.craft.controller.vo.CraftProcessSopRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessSopSaveReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessSopUpdateReqVO;
import com.badminton.mes.module.craft.convert.CraftProcessConvert;
import com.badminton.mes.module.craft.dal.entity.CraftProcessSopEntity;
import com.badminton.mes.module.craft.dal.redis.CraftCache;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.craft.dal.repository.CraftProcessSopRepository;
import com.badminton.mes.module.craft.dal.repository.CraftRouteDetailRepository;
import com.badminton.mes.module.craft.enums.CraftProcessChangeTypeEnum;
import com.badminton.mes.module.craft.enums.CraftRouteStatusEnum;
import com.badminton.mes.module.craft.service.CraftProcessAuditService;
import com.badminton.mes.module.craft.service.CraftProcessSopService;
import com.badminton.mes.module.craft.service.support.CraftPersistenceExceptionTranslator;
import com.badminton.mes.module.craft.service.support.CraftVersionValidator;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 工序 SOP 关联 Service 实现。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Service
public class CraftProcessSopServiceImpl implements CraftProcessSopService {

    private static final int SOP_CODE_MAX_LENGTH = 32;

    private static final String ACTIVE_SOP_CODE_CONSTRAINT = "uk_process_active_sop_code";

    private final CraftProcessRepository processRepository;

    private final CraftProcessSopRepository sopRepository;

    private final CraftRouteDetailRepository routeDetailRepository;

    private final CraftProcessAuditService auditService;

    private final CraftCache craftCache;

    /**
     * 构造器注入。
     *
     * @param processRepository 工序 Repository
     * @param sopRepository     工序 SOP Repository
     * @param routeDetailRepository 路线明细 Repository
     * @param auditService      工序变更审计服务
     * @param craftCache        工艺 Redis 缓存
     */
    public CraftProcessSopServiceImpl(CraftProcessRepository processRepository,
                                      CraftProcessSopRepository sopRepository,
                                      CraftRouteDetailRepository routeDetailRepository,
                                      CraftProcessAuditService auditService,
                                      CraftCache craftCache) {
        this.processRepository = processRepository;
        this.sopRepository = sopRepository;
        this.routeDetailRepository = routeDetailRepository;
        this.auditService = auditService;
        this.craftCache = craftCache;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProcessSop(Long processId, CraftProcessSopSaveReqVO reqVO) {
        requireProcess(processId);
        normalizeRequest(reqVO);
        validateNormalizedCodeLength(reqVO.getSopCode());
        validateSopCode(processId, reqVO.getSopCode(), null);

        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        CraftProcessSopEntity sop = new CraftProcessSopEntity();
        sop.setProcessId(processId);
        copyToEntity(reqVO, sop);
        sop.setCreateBy(operatorId);
        sop.setUpdateBy(operatorId);
        saveSop(sop);
        auditService.record(processId, CraftProcessChangeTypeEnum.SOP_BINDING,
                null, CraftProcessConvert.toSopRespVO(sop),
                defaultReason(reqVO.getChangeReason(), "新增工序 SOP"), operatorId);
        craftCache.evictProcessSopsAfterCommit(processId);
        return sop.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProcessSop(Long processId, Long sopId, CraftProcessSopUpdateReqVO reqVO) {
        requireProcess(processId);
        CraftProcessSopEntity sop = requireSopForUpdate(processId, sopId);
        CraftVersionValidator.validate(sop.getVersion(), reqVO.getVersion(),
                CraftErrorCodeConstants.PROCESS_SOP_CONCURRENT_MODIFICATION);
        normalizeRequest(reqVO);
        validateNormalizedCodeLength(reqVO.getSopCode());
        validateSopCode(processId, reqVO.getSopCode(), sopId);
        boolean disabling = CommonStatusEnum.ENABLED.getStatus().equals(sop.getStatus())
                && CommonStatusEnum.DISABLED.getStatus().equals(reqVO.getStatus());
        if (disabling) {
            validateNoEffectiveRouteReferences(sopId);
        }

        CraftProcessSopRespVO beforeSnapshot = CraftProcessConvert.toSopRespVO(sop);
        copyToEntity(reqVO, sop);
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        sop.setUpdateBy(operatorId);
        saveSop(sop);
        auditService.record(processId, CraftProcessChangeTypeEnum.SOP_BINDING,
                beforeSnapshot, CraftProcessConvert.toSopRespVO(sop),
                defaultReason(reqVO.getChangeReason(), "修改工序 SOP"), operatorId);
        craftCache.evictProcessSopsAfterCommit(processId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProcessSop(Long processId, Long sopId, Integer expectedVersion) {
        requireProcess(processId);
        CraftProcessSopEntity sop = requireSopForUpdate(processId, sopId);
        CraftVersionValidator.validate(sop.getVersion(), expectedVersion,
                CraftErrorCodeConstants.PROCESS_SOP_CONCURRENT_MODIFICATION);
        validateNoEffectiveRouteReferences(sopId);

        CraftProcessSopRespVO beforeSnapshot = CraftProcessConvert.toSopRespVO(sop);
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        sop.setDeleted(true);
        sop.setUpdateBy(operatorId);
        saveSop(sop);
        auditService.record(processId, CraftProcessChangeTypeEnum.SOP_BINDING,
                beforeSnapshot, null, "删除工序 SOP", operatorId);
        craftCache.evictProcessSopsAfterCommit(processId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CraftProcessSopRespVO> getProcessSops(Long processId) {
        List<CraftProcessSopRespVO> cached = craftCache.getProcessSops(processId).orElse(null);
        if (cached != null) {
            return cached;
        }

        requireProcess(processId);
        List<CraftProcessSopRespVO> result = CraftProcessConvert.toSopRespVOList(
                sopRepository.findByProcessIdAndDeletedFalseOrderByIdAsc(processId));
        craftCache.putProcessSops(processId, result);
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
     * 查询工序下未删除的 SOP。
     *
     * @param processId 工序主键
     * @param sopId     SOP 关联主键
     * @return SOP 实体
     */
    private CraftProcessSopEntity requireSop(Long processId, Long sopId) {
        return sopRepository.findByIdAndProcessIdAndDeletedFalse(sopId, processId)
                .orElseThrow(() -> new ServiceException(CraftErrorCodeConstants.PROCESS_SOP_NOT_EXISTS));
    }

    /**
     * 以写锁查询工序下未删除 SOP，和路线审核的引用锁串行化。
     *
     * @param processId 工序主键
     * @param sopId     SOP 主键
     * @return SOP 实体
     */
    private CraftProcessSopEntity requireSopForUpdate(Long processId, Long sopId) {
        return sopRepository.findByIdAndProcessIdAndDeletedFalseForUpdate(sopId, processId)
                .orElseThrow(() -> new ServiceException(CraftErrorCodeConstants.PROCESS_SOP_NOT_EXISTS));
    }

    /**
     * 校验 SOP 未被生效路线引用。
     *
     * @param sopId SOP 主键
     */
    private void validateNoEffectiveRouteReferences(Long sopId) {
        boolean referenced = routeDetailRepository.existsEffectiveRouteBySopId(
                sopId, CraftRouteStatusEnum.EFFECTIVE.getStatus());
        if (referenced) {
            throw new ServiceException(CraftErrorCodeConstants.PROCESS_SOP_REFERENCED_BY_EFFECTIVE_ROUTE);
        }
    }

    /**
     * 校验同一工序下 SOP 编码唯一。
     *
     * @param processId 工序主键
     * @param sopCode   SOP 编码
     * @param excludeId 排除的关联主键，创建时为 null
     */
    private void validateSopCode(Long processId, String sopCode, Long excludeId) {
        boolean exists = excludeId == null
                ? sopRepository.existsByProcessIdAndSopCodeAndDeletedFalse(processId, sopCode)
                : sopRepository.existsByProcessIdAndSopCodeAndIdNotAndDeletedFalse(
                        processId, sopCode, excludeId);
        if (exists) {
            throw new ServiceException(CraftErrorCodeConstants.PROCESS_SOP_CODE_DUPLICATE);
        }
    }

    /**
     * 保存 SOP 并转换可识别的并发或唯一约束异常。
     *
     * @param sop SOP 实体
     */
    private void saveSop(CraftProcessSopEntity sop) {
        try {
            sopRepository.saveAndFlush(sop);
        } catch (DataIntegrityViolationException exception) {
            CraftPersistenceExceptionTranslator.translateUniqueConstraint(exception,
                    ACTIVE_SOP_CODE_CONSTRAINT, CraftErrorCodeConstants.PROCESS_SOP_CODE_DUPLICATE);
        } catch (OptimisticLockingFailureException exception) {
            throw new ServiceException(CraftErrorCodeConstants.PROCESS_SOP_CONCURRENT_MODIFICATION);
        }
    }

    /**
     * 规范化 SOP 保存请求。
     *
     * @param reqVO 保存请求
     */
    private void normalizeRequest(CraftProcessSopSaveReqVO reqVO) {
        reqVO.setSopCode(reqVO.getSopCode().trim().toUpperCase(Locale.ROOT));
        reqVO.setSopName(reqVO.getSopName().trim());
        reqVO.setSopVersion(reqVO.getSopVersion().trim());
        reqVO.setFileUrl(reqVO.getFileUrl().trim());
        reqVO.setChangeReason(trimToNull(reqVO.getChangeReason()));
    }

    /**
     * 在规范化后再次保护数据库编码长度。
     *
     * @param sopCode 已规范化 SOP 编码
     */
    private void validateNormalizedCodeLength(String sopCode) {
        if (sopCode.length() > SOP_CODE_MAX_LENGTH) {
            throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR, "规范化后的 SOP 编码长度不能超过 32");
        }
    }

    /**
     * 复制 SOP 业务字段。
     *
     * @param reqVO 保存请求
     * @param sop   SOP 实体
     */
    private void copyToEntity(CraftProcessSopSaveReqVO reqVO, CraftProcessSopEntity sop) {
        sop.setSopCode(reqVO.getSopCode());
        sop.setSopName(reqVO.getSopName());
        sop.setSopVersion(reqVO.getSopVersion());
        sop.setFileUrl(reqVO.getFileUrl());
        sop.setStatus(reqVO.getStatus());
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
