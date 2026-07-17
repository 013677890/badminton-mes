package com.badminton.mes.module.craft.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.constants.CraftErrorCodeConstants;
import com.badminton.mes.module.craft.controller.vo.CraftProcessChangeLogPageReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessChangeLogRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessPageReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessRuleRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessSaveReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessStatusReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessUpdateReqVO;
import com.badminton.mes.module.craft.convert.CraftProcessConvert;
import com.badminton.mes.module.craft.dal.entity.CraftProcessChangeLogEntity;
import com.badminton.mes.module.craft.dal.entity.CraftProcessEntity;
import com.badminton.mes.module.craft.dal.entity.CraftQualityPlanReferenceEntity;
import com.badminton.mes.module.craft.dal.redis.CraftCache;
import com.badminton.mes.module.craft.dal.repository.CraftProcessChangeLogRepository;
import com.badminton.mes.module.craft.dal.repository.CraftProcessDefectReasonRepository;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.craft.dal.repository.CraftProcessSopRepository;
import com.badminton.mes.module.craft.dal.repository.CraftProcessSpecifications;
import com.badminton.mes.module.craft.dal.repository.CraftQualityPlanReferenceRepository;
import com.badminton.mes.module.craft.dal.repository.CraftRouteDetailRepository;
import com.badminton.mes.module.craft.enums.CraftProcessChangeTypeEnum;
import com.badminton.mes.module.craft.enums.CraftRouteStatusEnum;
import com.badminton.mes.module.craft.service.CraftProcessAuditService;
import com.badminton.mes.module.craft.service.CraftProcessService;
import com.badminton.mes.module.craft.service.CraftProcessWageReferenceQuery;
import com.badminton.mes.module.craft.service.dto.CraftProcessSnapshotDTO;
import com.badminton.mes.module.craft.service.support.CraftPersistenceExceptionTranslator;
import com.badminton.mes.module.craft.service.support.CraftVersionValidator;
import com.badminton.mes.module.equipment.dal.entity.EquipmentCategoryEntity;
import com.badminton.mes.module.equipment.dal.repository.EquipmentCategoryRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 工序主档 Service 实现。
 *
 * <p>负责工序档案、规则、状态、引用完整性和变更日志分页，不承载 SOP 与不良原因写入逻辑。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Service
public class CraftProcessServiceImpl implements CraftProcessService {

    private static final int PROCESS_CODE_MAX_LENGTH = 32;

    private static final String ACTIVE_PROCESS_CODE_CONSTRAINT = "uk_active_process_code";

    private static final String CREATE_REASON = "创建工序档案";

    private static final String DELETE_REASON = "删除工序档案";

    private final CraftProcessRepository processRepository;

    private final CraftProcessChangeLogRepository changeLogRepository;

    private final CraftProcessSopRepository sopRepository;

    private final CraftProcessDefectReasonRepository defectReasonRepository;

    private final CraftRouteDetailRepository routeDetailRepository;

    private final CraftQualityPlanReferenceRepository qualityPlanRepository;

    private final EquipmentCategoryRepository equipmentCategoryRepository;

    private final CraftProcessAuditService auditService;

    private final CraftCache craftCache;

    private final CraftProcessWageReferenceQuery wageReferenceQuery;

    /**
     * 构造器注入。
     *
     * @param processRepository           工序 Repository
     * @param changeLogRepository         工序变更日志 Repository
     * @param sopRepository               工序 SOP Repository
     * @param defectReasonRepository      工序不良原因 Repository
     * @param routeDetailRepository       工艺路线明细 Repository
     * @param qualityPlanRepository       质量检验方案只读 Repository
     * @param equipmentCategoryRepository 设备类别 Repository
     * @param auditService                工序变更审计服务
     * @param craftCache                  工艺 Redis 缓存
     * @param wageReferenceQuery          计件工资规则反向引用查询
     */
    public CraftProcessServiceImpl(CraftProcessRepository processRepository,
                                   CraftProcessChangeLogRepository changeLogRepository,
                                   CraftProcessSopRepository sopRepository,
                                   CraftProcessDefectReasonRepository defectReasonRepository,
                                   CraftRouteDetailRepository routeDetailRepository,
                                   CraftQualityPlanReferenceRepository qualityPlanRepository,
                                   EquipmentCategoryRepository equipmentCategoryRepository,
                                   CraftProcessAuditService auditService,
                                   CraftCache craftCache,
                                   CraftProcessWageReferenceQuery wageReferenceQuery) {
        this.processRepository = processRepository;
        this.changeLogRepository = changeLogRepository;
        this.sopRepository = sopRepository;
        this.defectReasonRepository = defectReasonRepository;
        this.routeDetailRepository = routeDetailRepository;
        this.qualityPlanRepository = qualityPlanRepository;
        this.equipmentCategoryRepository = equipmentCategoryRepository;
        this.auditService = auditService;
        this.craftCache = craftCache;
        this.wageReferenceQuery = wageReferenceQuery;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProcess(CraftProcessSaveReqVO reqVO) {
        // 先统一编码大小写与可空文本，再按数据库最终写入值执行长度、唯一性和关联校验。
        normalizeSaveRequest(reqVO);
        validateNormalizedCodeLength(reqVO.getProcessCode());
        validateProcessCode(reqVO.getProcessCode(), null);
        validateAssociations(reqVO.getQualityRequired(), reqVO.getQualityPlanId(),
                reqVO.getEquipmentCategoryId());

        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        CraftProcessEntity process = CraftProcessConvert.toEntity(reqVO);
        process.setStatus(CommonStatusEnum.ENABLED.getStatus());
        process.setCreateBy(operatorId);
        process.setUpdateBy(operatorId);
        // 主档必须先落库取得主键，后续审计日志才能建立稳定的工序关联。
        saveProcess(process);
        auditService.record(process.getId(), CraftProcessChangeTypeEnum.CREATE,
                null, CraftProcessConvert.toSnapshotDTO(process),
                defaultReason(reqVO.getChangeReason(), CREATE_REASON), operatorId);
        // 缓存失效注册到事务提交后，回滚时继续保留旧缓存，不暴露未提交状态。
        craftCache.evictProcessAfterCommit(process.getId());
        return process.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProcess(Long id, CraftProcessUpdateReqVO reqVO) {
        // 悲观写锁与版本号校验共同防止并发修改覆盖，锁还与路线审核引用检查串行化。
        CraftProcessEntity process = requireProcessForUpdate(id);
        CraftVersionValidator.validate(process.getVersion(), reqVO.getVersion(),
                CraftErrorCodeConstants.PROCESS_CONCURRENT_MODIFICATION);
        normalizeSaveRequest(reqVO);
        validateNormalizedCodeLength(reqVO.getProcessCode());
        validateProcessCode(reqVO.getProcessCode(), id);
        validateAssociations(reqVO.getQualityRequired(), reqVO.getQualityPlanId(),
                reqVO.getEquipmentCategoryId());
        validateWageRuleChange(process, reqVO);
        validateEffectiveRouteRuleChange(process, reqVO);

        // 修改前快照必须在实体字段覆盖前生成，供审计日志展示完整前后差异。
        CraftProcessSnapshotDTO beforeSnapshot = CraftProcessConvert.toSnapshotDTO(process);
        CraftProcessConvert.copyToEntity(reqVO, process);
        process.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        saveProcess(process);
        auditService.record(id, CraftProcessChangeTypeEnum.UPDATE,
                beforeSnapshot, CraftProcessConvert.toSnapshotDTO(process),
                reqVO.getChangeReason(), process.getUpdateBy());
        craftCache.evictProcessAfterCommit(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProcess(Long id, Integer expectedVersion) {
        CraftProcessEntity process = requireProcessForUpdate(id);
        CraftVersionValidator.validate(process.getVersion(), expectedVersion,
                CraftErrorCodeConstants.PROCESS_CONCURRENT_MODIFICATION);
        validateNoReferences(id);
        validateNoEnabledWageRuleReferences(id);

        // 删除采用软删除并保留删除前快照，历史路线和审计数据仍可追溯。
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        CraftProcessSnapshotDTO beforeSnapshot = CraftProcessConvert.toSnapshotDTO(process);
        process.setDeleted(true);
        process.setUpdateBy(operatorId);
        saveProcess(process);
        auditService.record(id, CraftProcessChangeTypeEnum.DELETE,
                beforeSnapshot, null, DELETE_REASON, operatorId);
        // 删除会影响主档、SOP、不良原因等聚合缓存，因此按聚合维度统一失效。
        craftCache.evictProcessAggregateAfterCommit(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProcessStatus(Long id, CraftProcessStatusReqVO reqVO) {
        CraftProcessEntity process = requireProcessForUpdate(id);
        CraftVersionValidator.validate(process.getVersion(), reqVO.getVersion(),
                CraftErrorCodeConstants.PROCESS_CONCURRENT_MODIFICATION);
        if (reqVO.getStatus().equals(process.getStatus())) {
            // 幂等状态请求无需写库、递增版本号或重复生成审计记录。
            return;
        }
        if (CommonStatusEnum.ENABLED.getStatus().equals(reqVO.getStatus())) {
            // 重新启用时重新确认外部档案仍然有效，避免恢复带有失效关联的工序。
            validateAssociations(process.getQualityRequired(), process.getQualityPlanId(),
                    process.getEquipmentCategoryId());
        } else {
            // 停用会影响已生效路线执行与计件结算，存在有效引用时必须拒绝。
            validateNoEffectiveRouteReferences(id);
            validateNoEnabledWageRuleReferences(id);
        }

        CraftProcessSnapshotDTO beforeSnapshot = CraftProcessConvert.toSnapshotDTO(process);
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        process.setStatus(reqVO.getStatus());
        process.setUpdateBy(operatorId);
        saveProcess(process);
        auditService.record(id, CraftProcessChangeTypeEnum.STATUS_CHANGE,
                beforeSnapshot, CraftProcessConvert.toSnapshotDTO(process),
                reqVO.getReason().trim(), operatorId);
        craftCache.evictProcessAfterCommit(id);
    }

    @Override
    @Transactional(readOnly = true)
    public CraftProcessRespVO getProcess(Long id) {
        // 详情采用旁路缓存；缓存不可用或未命中时再回源 MySQL。
        CraftProcessRespVO cached = craftCache.getProcess(id).orElse(null);
        if (cached != null) {
            return cached;
        }

        CraftProcessRespVO result = CraftProcessConvert.toRespVO(requireProcess(id));
        // 仅将已通过未删除校验的数据库结果回填缓存，避免缓存无效主档。
        craftCache.putProcess(result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CraftProcessRuleRespVO> getProcessRules(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        // 过滤非法值并去重，控制 IN 参数规模，同时保留调用方首次出现的顺序。
        List<Long> distinctIds = ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (distinctIds.isEmpty()) {
            return List.of();
        }

        // JPA 的 IN 查询不保证结果顺序，先构建索引，再按调用方请求顺序稳定返回。
        Map<Long, CraftProcessEntity> processById = processRepository
                .findByIdInAndStatusAndDeletedFalse(
                        distinctIds, CommonStatusEnum.ENABLED.getStatus())
                .stream()
                .collect(Collectors.toMap(
                        CraftProcessEntity::getId,
                        Function.identity(),
                        (firstProcess, ignoredProcess) -> firstProcess));
        return distinctIds.stream()
                .map(processById::get)
                .filter(Objects::nonNull)
                .map(CraftProcessConvert::toRuleRespVO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CraftProcessRespVO> getProcessPage(CraftProcessPageReqVO reqVO) {
        Specification<CraftProcessEntity> specification = CraftProcessSpecifications.page(reqVO);
        // 先统计总数以便空结果快速返回，并为越界页码修正提供依据。
        long total = processRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        int pageSize = reqVO.getPageSize();
        int pageNo = normalizePageNo(reqVO.getPageNo(), pageSize, total);
        // 工序编码作为业务稳定顺序，主键倒序用于编码相同时提供确定性兜底。
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize,
                Sort.by(Sort.Direction.ASC, "processCode").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<CraftProcessEntity> page = processRepository.findAll(specification, pageRequest);
        return PageResult.of(CraftProcessConvert.toRespVOList(page.getContent()), total, pageNo, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CraftProcessChangeLogRespVO> getProcessChangeLogPage(
            Long id, CraftProcessChangeLogPageReqVO reqVO) {
        // 审计日志允许查询已软删除工序，因此这里只校验主键物理记录是否存在。
        if (!processRepository.existsById(id)) {
            throw new ServiceException(CraftErrorCodeConstants.PROCESS_NOT_EXISTS);
        }
        long total = changeLogRepository.countByProcessIdAndDeletedFalse(id);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        int pageSize = reqVO.getPageSize();
        int pageNo = normalizePageNo(reqVO.getPageNo(), pageSize, total);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<CraftProcessChangeLogEntity> page =
                changeLogRepository.findByProcessIdAndDeletedFalse(id, pageRequest);
        return PageResult.of(CraftProcessConvert.toChangeLogRespVOList(page.getContent()),
                total, pageNo, pageSize);
    }

    /**
     * 查询未删除工序。
     *
     * @param id 工序主键
     * @return 工序实体
     */
    private CraftProcessEntity requireProcess(Long id) {
        return processRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(CraftErrorCodeConstants.PROCESS_NOT_EXISTS));
    }

    /**
     * 以写锁查询未删除工序，和路线审核的引用锁串行化。
     *
     * @param id 工序主键
     * @return 工序实体
     */
    private CraftProcessEntity requireProcessForUpdate(Long id) {
        return processRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(CraftErrorCodeConstants.PROCESS_NOT_EXISTS));
    }

    /**
     * 生效路线引用工序时，禁止原地改变会影响路线执行的控制规则。
     *
     * @param process 当前工序
     * @param reqVO   修改请求
     */
    private void validateEffectiveRouteRuleChange(
            CraftProcessEntity process, CraftProcessUpdateReqVO reqVO) {
        // 名称、备注等展示字段可原地修改；只拦截会改变现场执行语义的控制字段。
        boolean controlRuleChanged = !Objects.equals(process.getProcessType(), reqVO.getProcessType())
                || !Objects.equals(process.getStandardTimeSeconds(), reqVO.getStandardTimeSeconds())
                || !Objects.equals(process.getKeyProcess(), reqVO.getKeyProcess())
                || !Objects.equals(process.getQualityRequired(), reqVO.getQualityRequired())
                || !Objects.equals(process.getScanRequired(), reqVO.getScanRequired())
                || !Objects.equals(process.getPieceRateEnabled(), reqVO.getPieceRateEnabled())
                || !Objects.equals(process.getEquipmentCategoryId(), reqVO.getEquipmentCategoryId())
                || !Objects.equals(process.getQualityPlanId(), reqVO.getQualityPlanId());
        if (controlRuleChanged) {
            validateNoEffectiveRouteReferences(process.getId());
        }
    }

    /**
     * 关闭工序计件能力前校验不存在启用计件规则。
     *
     * @param process 当前工序
     * @param reqVO   修改请求
     */
    private void validateWageRuleChange(CraftProcessEntity process, CraftProcessUpdateReqVO reqVO) {
        boolean disablesPieceRate = Boolean.TRUE.equals(process.getPieceRateEnabled())
                && Boolean.FALSE.equals(reqVO.getPieceRateEnabled());
        if (disablesPieceRate) {
            validateNoEnabledWageRuleReferences(process.getId());
        }
    }

    /**
     * 校验工序不存在启用计件规则引用。
     *
     * @param processId 工序主键
     */
    private void validateNoEnabledWageRuleReferences(Long processId) {
        if (wageReferenceQuery.hasEnabledPieceRateRule(processId)) {
            throw new ServiceException(CraftErrorCodeConstants.PROCESS_REFERENCED_BY_ENABLED_WAGE_RULE);
        }
    }

    /**
     * 校验工序未被生效路线引用。
     *
     * @param processId 工序主键
     */
    private void validateNoEffectiveRouteReferences(Long processId) {
        // 查询直接下推数据库，仅判断是否存在生效路线，避免加载完整路线明细集合。
        boolean referenced = routeDetailRepository.existsEffectiveRouteByProcessId(
                processId, CraftRouteStatusEnum.EFFECTIVE.getStatus());
        if (referenced) {
            throw new ServiceException(CraftErrorCodeConstants.PROCESS_RULE_REFERENCED_BY_EFFECTIVE_ROUTE);
        }
    }

    /**
     * 校验工序编码唯一性。
     *
     * @param processCode 工序编码
     * @param excludeId   排除的工序主键，创建时为 null
     */
    private void validateProcessCode(String processCode, Long excludeId) {
        // 应用层查重用于给出友好提示，最终并发唯一性仍由数据库唯一约束兜底。
        boolean exists = excludeId == null
                ? processRepository.existsByProcessCodeAndDeletedFalse(processCode)
                : processRepository.existsByProcessCodeAndIdNotAndDeletedFalse(processCode, excludeId);
        if (exists) {
            throw new ServiceException(CraftErrorCodeConstants.PROCESS_CODE_DUPLICATE);
        }
    }

    /**
     * 校验质检方案与设备类别关联均可用。
     *
     * @param qualityRequired     是否需要质检
     * @param qualityPlanId       检验方案主键
     * @param equipmentCategoryId 设备类别主键
     */
    private void validateAssociations(Boolean qualityRequired, Long qualityPlanId,
                                      Long equipmentCategoryId) {
        if (Boolean.TRUE.equals(qualityRequired) && qualityPlanId == null) {
            throw new ServiceException(CraftErrorCodeConstants.PROCESS_QUALITY_PLAN_REQUIRED);
        }
        if (qualityPlanId != null && !qualityPlanRepository.existsByIdAndPlanStatusAndDeletedFalse(
                qualityPlanId, CraftQualityPlanReferenceEntity.PLAN_STATUS_EFFECTIVE)) {
            throw new ServiceException(CraftErrorCodeConstants.PROCESS_QUALITY_PLAN_NOT_AVAILABLE);
        }
        if (equipmentCategoryId == null) {
            return;
        }

        // 锁定设备类别记录，使工序保存与类别停用操作串行，避免校验后立即失效。
        EquipmentCategoryEntity category = equipmentCategoryRepository
                .findByIdAndDeletedFalseForUpdate(equipmentCategoryId)
                .orElseThrow(() -> new ServiceException(
                        CraftErrorCodeConstants.PROCESS_EQUIPMENT_CATEGORY_NOT_AVAILABLE));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(category.getStatus())) {
            throw new ServiceException(CraftErrorCodeConstants.PROCESS_EQUIPMENT_CATEGORY_NOT_AVAILABLE);
        }
    }

    /**
     * 校验工序不存在路线或子资源引用。
     *
     * @param processId 工序主键
     */
    private void validateNoReferences(Long processId) {
        // 路线引用是删除的第一层硬约束，使用 exists 查询避免拉取无关明细。
        if (routeDetailRepository.existsByProcessIdAndDeletedFalse(processId)) {
            throw new ServiceException(CraftErrorCodeConstants.PROCESS_REFERENCED_BY_ROUTE);
        }
        boolean hasBindings = sopRepository.existsByProcessIdAndDeletedFalse(processId)
                || defectReasonRepository.existsByProcessIdAndDeletedFalse(processId);
        if (hasBindings) {
            throw new ServiceException(CraftErrorCodeConstants.PROCESS_HAS_ACTIVE_BINDINGS);
        }
    }

    /**
     * 保存工序并转换可识别的并发或唯一约束异常。
     *
     * @param process 工序实体
     */
    private void saveProcess(CraftProcessEntity process) {
        try {
            // 立即 flush，让唯一约束与乐观锁异常在当前业务方法内抛出并转换。
            processRepository.saveAndFlush(process);
        } catch (DataIntegrityViolationException exception) {
            CraftPersistenceExceptionTranslator.translateUniqueConstraint(exception,
                    ACTIVE_PROCESS_CODE_CONSTRAINT, CraftErrorCodeConstants.PROCESS_CODE_DUPLICATE);
        } catch (OptimisticLockingFailureException exception) {
            throw new ServiceException(CraftErrorCodeConstants.PROCESS_CONCURRENT_MODIFICATION);
        }
    }

    /**
     * 规范化工序保存请求。
     *
     * @param reqVO 保存请求
     */
    private void normalizeSaveRequest(CraftProcessSaveReqVO reqVO) {
        reqVO.setProcessCode(reqVO.getProcessCode().trim().toUpperCase(Locale.ROOT));
        reqVO.setProcessName(reqVO.getProcessName().trim());
        reqVO.setProcessType(reqVO.getProcessType().trim().toUpperCase(Locale.ROOT));
        reqVO.setRemark(trimToNull(reqVO.getRemark()));
        reqVO.setChangeReason(trimToNull(reqVO.getChangeReason()));
    }

    /**
     * 在规范化后再次保护数据库编码长度。
     *
     * @param processCode 已规范化工序编码
     */
    private void validateNormalizedCodeLength(String processCode) {
        if (processCode.length() > PROCESS_CODE_MAX_LENGTH) {
            throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR, "规范化后的工序编码长度不能超过 32");
        }
    }

    /**
     * 将请求页码修正到有效范围。
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
