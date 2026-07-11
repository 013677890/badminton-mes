package com.badminton.mes.module.wage.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.dal.entity.CraftProcessEntity;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.production.dal.entity.ProductEntity;
import com.badminton.mes.module.production.dal.repository.ProductRepository;
import com.badminton.mes.module.wage.constants.WageErrorCodeConstants;
import com.badminton.mes.module.wage.controller.vo.PieceRateRulePageReqVO;
import com.badminton.mes.module.wage.controller.vo.PieceRateRuleRespVO;
import com.badminton.mes.module.wage.controller.vo.PieceRateRuleSaveReqVO;
import com.badminton.mes.module.wage.controller.vo.PieceRateRuleStatusReqVO;
import com.badminton.mes.module.wage.controller.vo.PieceRateRuleUpdateReqVO;
import com.badminton.mes.module.wage.controller.vo.WageRuleChangeLogPageReqVO;
import com.badminton.mes.module.wage.controller.vo.WageRuleChangeLogRespVO;
import com.badminton.mes.module.wage.convert.WageConvert;
import com.badminton.mes.module.wage.dal.entity.PieceRateRuleEntity;
import com.badminton.mes.module.wage.dal.entity.WageRuleChangeLogEntity;
import com.badminton.mes.module.wage.dal.repository.PieceRateRuleRepository;
import com.badminton.mes.module.wage.dal.repository.WageRuleChangeLogRepository;
import com.badminton.mes.module.wage.dal.repository.WageSpecifications;
import com.badminton.mes.module.wage.enums.WageRuleChangeTypeEnum;
import com.badminton.mes.module.wage.service.PieceRateRuleService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** 计件规则业务实现。 */
@Service
public class PieceRateRuleServiceImpl implements PieceRateRuleService {

    private final PieceRateRuleRepository ruleRepository;
    private final WageRuleChangeLogRepository changeLogRepository;
    private final CraftProcessRepository processRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    /** 构造器注入。 */
    public PieceRateRuleServiceImpl(PieceRateRuleRepository ruleRepository,
                                    WageRuleChangeLogRepository changeLogRepository,
                                    CraftProcessRepository processRepository,
                                    ProductRepository productRepository,
                                    ObjectMapper objectMapper) {
        this.ruleRepository = ruleRepository;
        this.changeLogRepository = changeLogRepository;
        this.processRepository = processRepository;
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRule(PieceRateRuleSaveReqVO reqVO) {
        validateDateRange(reqVO.getEffectiveStart(), reqVO.getEffectiveEnd());
        validateAndLockReferences(reqVO.getProcessId(), reqVO.getProductId());
        validateNoOverlap(reqVO, null);
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        PieceRateRuleEntity entity = WageConvert.toRuleEntity(reqVO);
        entity.setCreateBy(operatorId);
        entity.setUpdateBy(operatorId);
        saveRule(entity);
        recordChange(entity.getId(), WageRuleChangeTypeEnum.CREATE, null, entity,
                defaultReason(reqVO.getChangeReason(), "创建计件规则"), operatorId);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRule(Long id, PieceRateRuleUpdateReqVO reqVO) {
        validateDateRange(reqVO.getEffectiveStart(), reqVO.getEffectiveEnd());
        validateAndLockReferences(reqVO.getProcessId(), reqVO.getProductId());
        PieceRateRuleEntity entity = requireRuleForUpdate(id);
        validateVersion(entity.getVersion(), reqVO.getVersion());
        validateNoOverlap(reqVO, id);
        String before = snapshot(entity);
        WageConvert.copyRule(reqVO, entity);
        entity.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        saveRule(entity);
        recordChange(id, WageRuleChangeTypeEnum.UPDATE, before, entity,
                defaultReason(reqVO.getChangeReason(), "修改计件规则"), entity.getUpdateBy());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id, Integer version) {
        PieceRateRuleEntity entity = requireRuleForUpdate(id);
        validateVersion(entity.getVersion(), version);
        String before = snapshot(entity);
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        entity.setDeleted(true);
        entity.setUpdateBy(operatorId);
        saveRule(entity);
        recordChange(id, WageRuleChangeTypeEnum.DELETE, before, null,
                "删除计件规则", operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRuleStatus(Long id, PieceRateRuleStatusReqVO reqVO) {
        PieceRateRuleEntity currentRule = requireRule(id);
        if (CommonStatusEnum.ENABLED.getStatus().equals(reqVO.getStatus())) {
            validateAndLockReferences(currentRule.getProcessId(), currentRule.getProductId());
        }
        PieceRateRuleEntity entity = requireRuleForUpdate(id);
        validateVersion(entity.getVersion(), reqVO.getVersion());
        validateReferencesUnchanged(currentRule, entity);
        if (Objects.equals(entity.getStatus(), reqVO.getStatus())) {
            return;
        }
        String before = snapshot(entity);
        entity.setStatus(reqVO.getStatus());
        entity.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        saveRule(entity);
        recordChange(id, WageRuleChangeTypeEnum.STATUS, before, entity,
                reqVO.getReason().trim(), entity.getUpdateBy());
    }

    @Override
    @Transactional(readOnly = true)
    public PieceRateRuleRespVO getRule(Long id) {
        return WageConvert.toRuleRespVO(requireRule(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PieceRateRuleRespVO> getRulePage(PieceRateRulePageReqVO reqVO) {
        var specification = WageSpecifications.rulePage(reqVO);
        long total = ruleRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }
        int pageNo = normalizePageNo(reqVO.getPageNo(), reqVO.getPageSize(), total);
        PageRequest pageable = PageRequest.of(pageNo - 1, reqVO.getPageSize(),
                Sort.by(Sort.Direction.DESC, "effectiveStart").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<PieceRateRuleEntity> page = ruleRepository.findAll(specification, pageable);
        return PageResult.of(WageConvert.toRuleRespVOList(page.getContent()),
                total, pageNo, reqVO.getPageSize());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WageRuleChangeLogRespVO> getRuleChangeLogPage(
            Long id, WageRuleChangeLogPageReqVO reqVO) {
        if (!ruleRepository.existsById(id)) {
            throw new ServiceException(WageErrorCodeConstants.RULE_NOT_EXISTS);
        }
        long total = changeLogRepository.countByRuleIdAndDeletedFalse(id);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }
        int pageNo = normalizePageNo(reqVO.getPageNo(), reqVO.getPageSize(), total);
        Page<WageRuleChangeLogEntity> page = changeLogRepository.findByRuleIdAndDeletedFalse(
                id, PageRequest.of(pageNo - 1, reqVO.getPageSize(), Sort.by(Sort.Direction.DESC, "id")));
        List<WageRuleChangeLogRespVO> records = page.getContent().stream()
                .map(WageConvert::toRuleLogRespVO).toList();
        return PageResult.of(records, total, pageNo, reqVO.getPageSize());
    }

    /** 校验日期范围。 */
    private void validateDateRange(LocalDate start, LocalDate end) {
        if (end != null && end.isBefore(start)) {
            throw new ServiceException(WageErrorCodeConstants.RULE_DATE_INVALID);
        }
    }

    /** 锁定并校验工序、产品引用。 */
    private void validateAndLockReferences(Long processId, Long productId) {
        CraftProcessEntity process = processRepository.findByIdAndDeletedFalseForUpdate(processId)
                .orElseThrow(() -> new ServiceException(WageErrorCodeConstants.PROCESS_NOT_PIECE_RATE));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(process.getStatus())
                || !Boolean.TRUE.equals(process.getPieceRateEnabled())) {
            throw new ServiceException(WageErrorCodeConstants.PROCESS_NOT_PIECE_RATE);
        }
        if (productId == null) {
            return;
        }
        List<ProductEntity> products = productRepository.findAllByIdInForUpdateOrderByIdAsc(List.of(productId));
        if (products.size() != 1 || !CommonStatusEnum.ENABLED.getStatus().equals(products.get(0).getStatus())) {
            throw new ServiceException(WageErrorCodeConstants.PRODUCT_NOT_AVAILABLE);
        }
    }

    /** 校验生效区间不重叠。 */
    private void validateNoOverlap(PieceRateRuleSaveReqVO reqVO, Long excludeId) {
        if (ruleRepository.existsOverlapping(reqVO.getProcessId(), reqVO.getProductId(),
                reqVO.getEffectiveStart(), reqVO.getEffectiveEnd(), excludeId)) {
            throw new ServiceException(WageErrorCodeConstants.RULE_PERIOD_OVERLAP);
        }
    }

    /** 查询未删除规则。 */
    private PieceRateRuleEntity requireRule(Long id) {
        return ruleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(WageErrorCodeConstants.RULE_NOT_EXISTS));
    }

    /** 写锁查询未删除规则。 */
    private PieceRateRuleEntity requireRuleForUpdate(Long id) {
        return ruleRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(WageErrorCodeConstants.RULE_NOT_EXISTS));
    }

    /** 校验预读后规则引用未发生变化，避免按过期引用获取锁。 */
    private void validateReferencesUnchanged(PieceRateRuleEntity currentRule, PieceRateRuleEntity lockedRule) {
        boolean referencesChanged = !Objects.equals(currentRule.getProcessId(), lockedRule.getProcessId())
                || !Objects.equals(currentRule.getProductId(), lockedRule.getProductId());
        if (referencesChanged) {
            throw new ServiceException(WageErrorCodeConstants.RULE_CONCURRENT_MODIFICATION);
        }
    }

    /** 校验客户端预期版本。 */
    private void validateVersion(Integer current, Integer expected) {
        if (!Objects.equals(current, expected)) {
            throw new ServiceException(WageErrorCodeConstants.RULE_CONCURRENT_MODIFICATION);
        }
    }

    /** 保存规则并转换约束、并发异常。 */
    private void saveRule(PieceRateRuleEntity entity) {
        try {
            ruleRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(WageErrorCodeConstants.RULE_PERIOD_OVERLAP);
        } catch (OptimisticLockingFailureException exception) {
            throw new ServiceException(WageErrorCodeConstants.RULE_CONCURRENT_MODIFICATION);
        }
    }

    /** 记录规则审计日志。 */
    private void recordChange(Long ruleId, WageRuleChangeTypeEnum type, String before,
                              PieceRateRuleEntity after, String reason, Long operatorId) {
        WageRuleChangeLogEntity log = new WageRuleChangeLogEntity();
        log.setRuleId(ruleId);
        log.setChangeType(type.name());
        log.setBeforeSnapshot(before);
        log.setAfterSnapshot(after == null ? null : snapshot(after));
        log.setChangeReason(reason);
        log.setOperateBy(operatorId);
        changeLogRepository.save(log);
    }

    /** 将规则序列化为审计快照。 */
    private String snapshot(PieceRateRuleEntity entity) {
        try {
            return objectMapper.writeValueAsString(WageConvert.toRuleRespVO(entity));
        } catch (JacksonException exception) {
            throw new ServiceException(GlobalErrorCodeConstants.SYSTEM_ERROR, "计件规则审计快照序列化失败");
        }
    }

    /** 规范化页码。 */
    private int normalizePageNo(int requested, int pageSize, long total) {
        return Math.min(requested, (int) ((total + pageSize - 1) / pageSize));
    }

    /** 返回非空白原因或默认原因。 */
    private String defaultReason(String reason, String defaultValue) {
        return StringUtils.hasText(reason) ? reason.trim() : defaultValue;
    }
}
