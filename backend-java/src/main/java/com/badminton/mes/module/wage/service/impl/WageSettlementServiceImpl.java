package com.badminton.mes.module.wage.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.dal.entity.CraftProcessEntity;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.system.dal.entity.UserEntity;
import com.badminton.mes.module.system.dal.repository.UserRepository;
import com.badminton.mes.module.wage.constants.WageErrorCodeConstants;
import com.badminton.mes.module.wage.controller.vo.EmployeeWageSummaryRespVO;
import com.badminton.mes.module.wage.controller.vo.ProcessWageSummaryRespVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementActionReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementAdjustReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementAuditLogPageReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementAuditLogRespVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementCalculateReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementDetailPageReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementDetailRespVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementPageReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementRespVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementVersionReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSummaryReqVO;
import com.badminton.mes.module.wage.convert.WageConvert;
import com.badminton.mes.module.wage.dal.entity.WageSettlementAuditLogEntity;
import com.badminton.mes.module.wage.dal.entity.WageSettlementDetailEntity;
import com.badminton.mes.module.wage.dal.entity.WageSettlementEntity;
import com.badminton.mes.module.wage.dal.entity.WageWorkRecordEntity;
import com.badminton.mes.module.wage.dal.repository.EmployeeWageSummaryProjection;
import com.badminton.mes.module.wage.dal.repository.ProcessWageSummaryProjection;
import com.badminton.mes.module.wage.dal.repository.WageSettlementAuditLogRepository;
import com.badminton.mes.module.wage.dal.repository.WageSettlementDetailRepository;
import com.badminton.mes.module.wage.dal.repository.WageSettlementRepository;
import com.badminton.mes.module.wage.dal.repository.WageSpecifications;
import com.badminton.mes.module.wage.dal.repository.WageWorkRecordRepository;
import com.badminton.mes.module.wage.enums.WageSettlementActionEnum;
import com.badminton.mes.module.wage.enums.WageSettlementStatusEnum;
import com.badminton.mes.module.wage.service.WageSettlementAuditService;
import com.badminton.mes.module.wage.service.WageSettlementCalculator;
import com.badminton.mes.module.wage.service.WageSettlementService;
import com.badminton.mes.module.wage.service.dto.WageCalculationResult;
import com.badminton.mes.module.wage.service.support.WageAmountUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** 计件工资结算服务实现。 */
@Service
public class WageSettlementServiceImpl implements WageSettlementService {

    private static final int MAX_PERIOD_DAYS = 31;
    private static final int MAX_RECORD_COUNT = 5_000;
    private static final int QUERY_RECORD_COUNT = MAX_RECORD_COUNT + 1;
    private static final int MAX_SUMMARY_RESULT_COUNT = 1_000;
    private static final int QUERY_SUMMARY_RESULT_COUNT = MAX_SUMMARY_RESULT_COUNT + 1;
    private static final BigDecimal MAX_SETTLEMENT_QUANTITY = new BigDecimal("99999999999999.9999");

    private final WageSettlementRepository settlementRepository;
    private final WageSettlementDetailRepository detailRepository;
    private final WageSettlementAuditLogRepository auditLogRepository;
    private final WageWorkRecordRepository workRecordRepository;
    private final WageSettlementCalculator calculator;
    private final WageSettlementAuditService auditService;
    private final UserRepository userRepository;
    private final CraftProcessRepository processRepository;
    private final ObjectMapper objectMapper;

    /** 构造器注入。 */
    public WageSettlementServiceImpl(WageSettlementRepository settlementRepository,
                                     WageSettlementDetailRepository detailRepository,
                                     WageSettlementAuditLogRepository auditLogRepository,
                                     WageWorkRecordRepository workRecordRepository,
                                     WageSettlementCalculator calculator,
                                     WageSettlementAuditService auditService,
                                     UserRepository userRepository,
                                     CraftProcessRepository processRepository,
                                     ObjectMapper objectMapper) {
        this.settlementRepository = settlementRepository;
        this.detailRepository = detailRepository;
        this.auditLogRepository = auditLogRepository;
        this.workRecordRepository = workRecordRepository;
        this.calculator = calculator;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.processRepository = processRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long calculateSettlement(WageSettlementCalculateReqVO reqVO) {
        validateSettlementPeriod(reqVO.getPeriodStart(), reqVO.getPeriodEnd());
        List<Long> employeeIds = normalizeIds(reqVO.getEmployeeIds());
        validateEmployeeScope(employeeIds);
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();

        WageSettlementEntity settlement = new WageSettlementEntity();
        settlement.setSettlementNo(generateSettlementNo());
        settlement.setPeriodStart(reqVO.getPeriodStart());
        settlement.setPeriodEnd(reqVO.getPeriodEnd());
        settlement.setEmployeeScope(writeScope(employeeIds));
        settlement.setSettlementStatus(WageSettlementStatusEnum.DRAFT.getStatus());
        settlement.setTotalQualifiedQuantity(BigDecimal.ZERO);
        settlement.setTotalDefectQuantity(BigDecimal.ZERO);
        settlement.setTotalAmountBasis(0L);
        settlement.setCreateBy(operatorId);
        settlement.setUpdateBy(operatorId);
        saveSettlement(settlement);

        WageCalculationResult result = calculateLockedRecords(settlement.getId(),
                settlement.getPeriodStart(), settlement.getPeriodEnd(), employeeIds);
        saveDetails(result.details());
        applyTotals(settlement, result);
        saveSettlement(settlement);
        auditService.recordSettlement(settlement.getId(), WageSettlementActionEnum.CALCULATE,
                null, WageSettlementStatusEnum.DRAFT.getStatus(),
                trimToNull(reqVO.getReason()), operatorId);
        return settlement.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recalculateSettlement(Long id, WageSettlementVersionReqVO reqVO) {
        WageSettlementEntity settlement = requireSettlementForUpdate(id);
        validateVersion(settlement, reqVO.getVersion());
        requireStatus(settlement, WageSettlementStatusEnum.DRAFT, WageSettlementStatusEnum.REJECTED);
        Integer fromStatus = settlement.getSettlementStatus();

        List<Long> previousRecordIds = detailRepository.findWorkRecordIdsBySettlementId(id);
        if (!previousRecordIds.isEmpty()) {
            workRecordRepository.findAllByIdInForUpdateOrderByIdAsc(previousRecordIds);
        }
        detailRepository.deactivateBySettlementId(id);
        List<Long> employeeIds = readScope(settlement.getEmployeeScope());
        WageCalculationResult result = calculateLockedRecords(id, settlement.getPeriodStart(),
                settlement.getPeriodEnd(), employeeIds);
        saveDetails(result.details());

        settlement.setSettlementStatus(WageSettlementStatusEnum.DRAFT.getStatus());
        settlement.setSubmitBy(null);
        settlement.setSubmitTime(null);
        settlement.setAuditBy(null);
        settlement.setAuditTime(null);
        settlement.setAuditReason(null);
        settlement.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        applyTotals(settlement, result);
        saveSettlement(settlement);
        auditService.recordSettlement(id, WageSettlementActionEnum.RECALCULATE,
                fromStatus, WageSettlementStatusEnum.DRAFT.getStatus(),
                "重新计算工资结算", settlement.getUpdateBy());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitSettlement(Long id, WageSettlementActionReqVO reqVO) {
        WageSettlementEntity settlement = requireSettlementForUpdate(id);
        validateVersion(settlement, reqVO.getVersion());
        requireStatus(settlement, WageSettlementStatusEnum.DRAFT);
        if (detailRepository.countBySettlementIdAndActiveTrueAndDeletedFalse(id) == 0) {
            throw new ServiceException(WageErrorCodeConstants.SETTLEMENT_NO_ELIGIBLE_RECORD);
        }
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        settlement.setSettlementStatus(WageSettlementStatusEnum.PENDING.getStatus());
        settlement.setSubmitBy(operatorId);
        settlement.setSubmitTime(LocalDateTime.now());
        settlement.setUpdateBy(operatorId);
        saveSettlement(settlement);
        auditService.recordSettlement(id, WageSettlementActionEnum.SUBMIT,
                WageSettlementStatusEnum.DRAFT.getStatus(), WageSettlementStatusEnum.PENDING.getStatus(),
                trimToNull(reqVO.getReason()), operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveSettlement(Long id, WageSettlementActionReqVO reqVO) {
        auditSettlement(id, reqVO, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectSettlement(Long id, WageSettlementActionReqVO reqVO) {
        if (!StringUtils.hasText(reqVO.getReason())) {
            throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR, "驳回原因不能为空");
        }
        auditSettlement(id, reqVO, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustDetail(Long id, Long detailId, WageSettlementAdjustReqVO reqVO) {
        WageSettlementEntity settlement = requireSettlementForUpdate(id);
        validateVersion(settlement, reqVO.getSettlementVersion());
        requireStatus(settlement, WageSettlementStatusEnum.DRAFT);
        WageSettlementDetailEntity detail = detailRepository
                .findByIdAndSettlementIdAndActiveTrueAndDeletedFalse(detailId, id)
                .orElseThrow(() -> new ServiceException(WageErrorCodeConstants.SETTLEMENT_DETAIL_NOT_EXISTS));
        long beforeAmount = detail.getFinalAmountBasis();
        long afterAmount = WageAmountUtils.toAmountBasis(reqVO.getAdjustedAmount());
        detail.setAdjustedAmountBasis(afterAmount);
        detail.setFinalAmountBasis(afterAmount);
        detailRepository.saveAndFlush(detail);
        try {
            settlement.setTotalAmountBasis(Math.addExact(
                    Math.subtractExact(settlement.getTotalAmountBasis(), beforeAmount), afterAmount));
        } catch (ArithmeticException exception) {
            throw new ServiceException(WageErrorCodeConstants.AMOUNT_OUT_OF_RANGE);
        }
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        settlement.setUpdateBy(operatorId);
        saveSettlement(settlement);
        auditService.recordAdjustment(id, detailId, beforeAmount, afterAmount,
                reqVO.getReason().trim(), operatorId);
    }

    @Override
    @Transactional(readOnly = true)
    public WageSettlementRespVO getSettlement(Long id) {
        return WageConvert.toSettlementRespVO(requireSettlement(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WageSettlementRespVO> getSettlementPage(WageSettlementPageReqVO reqVO) {
        validateOptionalPagePeriod(reqVO);
        var specification = WageSpecifications.settlementPage(reqVO);
        long total = settlementRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }
        int pageNo = normalizePageNo(reqVO.getPageNo(), reqVO.getPageSize(), total);
        Page<WageSettlementEntity> page = settlementRepository.findAll(specification,
                PageRequest.of(pageNo - 1, reqVO.getPageSize(), Sort.by(Sort.Direction.DESC, "id")));
        List<WageSettlementRespVO> list = page.getContent().stream()
                .map(WageConvert::toSettlementRespVO).toList();
        return PageResult.of(list, total, pageNo, reqVO.getPageSize());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WageSettlementDetailRespVO> getDetailPage(
            Long id, WageSettlementDetailPageReqVO reqVO) {
        requireSettlement(id);
        long total = detailRepository.countBySettlementIdAndActiveTrueAndDeletedFalse(id);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }
        int pageNo = normalizePageNo(reqVO.getPageNo(), reqVO.getPageSize(), total);
        Page<WageSettlementDetailEntity> page = detailRepository
                .findBySettlementIdAndActiveTrueAndDeletedFalse(id,
                        PageRequest.of(pageNo - 1, reqVO.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
        List<WageSettlementDetailRespVO> list = page.getContent().stream()
                .map(WageConvert::toDetailRespVO).toList();
        return PageResult.of(list, total, pageNo, reqVO.getPageSize());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WageSettlementAuditLogRespVO> getAuditLogPage(
            Long id, WageSettlementAuditLogPageReqVO reqVO) {
        if (!settlementRepository.existsById(id)) {
            throw new ServiceException(WageErrorCodeConstants.SETTLEMENT_NOT_EXISTS);
        }
        long total = auditLogRepository.countBySettlementIdAndDeletedFalse(id);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }
        int pageNo = normalizePageNo(reqVO.getPageNo(), reqVO.getPageSize(), total);
        Page<WageSettlementAuditLogEntity> page = auditLogRepository.findBySettlementIdAndDeletedFalse(
                id, PageRequest.of(pageNo - 1, reqVO.getPageSize(), Sort.by(Sort.Direction.DESC, "id")));
        List<WageSettlementAuditLogRespVO> list = page.getContent().stream()
                .map(WageConvert::toAuditLogRespVO).toList();
        return PageResult.of(list, total, pageNo, reqVO.getPageSize());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeWageSummaryRespVO> summarizeEmployees(WageSummaryReqVO reqVO) {
        validateSummaryPeriod(reqVO);
        List<Long> ids = normalizeIds(reqVO.getIds());
        boolean allIds = ids.isEmpty();
        List<EmployeeWageSummaryProjection> summaries = detailRepository.summarizeEmployees(
                WageSettlementStatusEnum.APPROVED.getStatus(), reqVO.getPeriodStart(), reqVO.getPeriodEnd(),
                allIds, queryIds(ids), PageRequest.of(0, QUERY_SUMMARY_RESULT_COUNT));
        validateSummaryResultCount(summaries.size());
        Map<Long, UserEntity> users = userRepository.findAllById(summaries.stream()
                        .map(EmployeeWageSummaryProjection::getEmployeeId).toList()).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        return summaries.stream().map(summary -> toEmployeeSummary(summary, users)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcessWageSummaryRespVO> summarizeProcesses(WageSummaryReqVO reqVO) {
        validateSummaryPeriod(reqVO);
        List<Long> ids = normalizeIds(reqVO.getIds());
        boolean allIds = ids.isEmpty();
        List<ProcessWageSummaryProjection> summaries = detailRepository.summarizeProcesses(
                WageSettlementStatusEnum.APPROVED.getStatus(), reqVO.getPeriodStart(), reqVO.getPeriodEnd(),
                allIds, queryIds(ids), PageRequest.of(0, QUERY_SUMMARY_RESULT_COUNT));
        validateSummaryResultCount(summaries.size());
        Map<Long, CraftProcessEntity> processes = processRepository.findAllById(summaries.stream()
                        .map(ProcessWageSummaryProjection::getProcessId).toList()).stream()
                .collect(Collectors.toMap(CraftProcessEntity::getId, Function.identity()));
        return summaries.stream().map(summary -> toProcessSummary(summary, processes)).toList();
    }

    /** 审核通过或驳回待审核结算。 */
    private void auditSettlement(Long id, WageSettlementActionReqVO reqVO, boolean approved) {
        WageSettlementEntity settlement = requireSettlementForUpdate(id);
        validateVersion(settlement, reqVO.getVersion());
        requireStatus(settlement, WageSettlementStatusEnum.PENDING);
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        WageSettlementStatusEnum target = approved
                ? WageSettlementStatusEnum.APPROVED : WageSettlementStatusEnum.REJECTED;
        if (!approved) {
            // 驳回后明细仅作为历史快照保留，并释放来源报工供后续结算重新占用。
            detailRepository.deactivateBySettlementId(id);
        }
        settlement.setSettlementStatus(target.getStatus());
        settlement.setAuditBy(operatorId);
        settlement.setAuditTime(LocalDateTime.now());
        settlement.setAuditReason(trimToNull(reqVO.getReason()));
        settlement.setUpdateBy(operatorId);
        saveSettlement(settlement);
        auditService.recordSettlement(id, approved ? WageSettlementActionEnum.APPROVE
                        : WageSettlementActionEnum.REJECT,
                WageSettlementStatusEnum.PENDING.getStatus(), target.getStatus(),
                trimToNull(reqVO.getReason()), operatorId);
    }

    /** 锁定可结算报工并执行计算。 */
    private WageCalculationResult calculateLockedRecords(Long settlementId, LocalDate periodStart,
                                                          LocalDate periodEnd, List<Long> employeeIds) {
        boolean allEmployees = employeeIds.isEmpty();
        List<WageWorkRecordEntity> records = workRecordRepository.findEligibleForUpdate(
                periodStart, periodEnd, allEmployees, queryIds(employeeIds),
                PageRequest.of(0, QUERY_RECORD_COUNT));
        if (records.isEmpty()) {
            throw new ServiceException(WageErrorCodeConstants.SETTLEMENT_NO_ELIGIBLE_RECORD);
        }
        if (records.size() > MAX_RECORD_COUNT) {
            throw new ServiceException(WageErrorCodeConstants.SETTLEMENT_RECORD_LIMIT_EXCEEDED);
        }
        return calculator.calculate(settlementId, records, periodStart, periodEnd);
    }

    /** 保存计算明细并转换并发唯一键冲突。 */
    private void saveDetails(List<WageSettlementDetailEntity> details) {
        try {
            detailRepository.saveAllAndFlush(details);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(WageErrorCodeConstants.WORK_RECORD_ALREADY_SETTLED);
        }
    }

    /** 将计算合计写入结算批次。 */
    private void applyTotals(WageSettlementEntity settlement, WageCalculationResult result) {
        validateSettlementQuantity(result.totalQualifiedQuantity());
        validateSettlementQuantity(result.totalDefectQuantity());
        settlement.setTotalQualifiedQuantity(result.totalQualifiedQuantity());
        settlement.setTotalDefectQuantity(result.totalDefectQuantity());
        settlement.setTotalAmountBasis(result.totalAmountBasis());
    }

    /** 保存结算并转换乐观锁异常。 */
    private void saveSettlement(WageSettlementEntity settlement) {
        try {
            settlementRepository.saveAndFlush(settlement);
        } catch (OptimisticLockingFailureException exception) {
            throw new ServiceException(WageErrorCodeConstants.SETTLEMENT_CONCURRENT_MODIFICATION);
        }
    }

    /** 查询未删除结算。 */
    private WageSettlementEntity requireSettlement(Long id) {
        return settlementRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(WageErrorCodeConstants.SETTLEMENT_NOT_EXISTS));
    }

    /** 写锁查询未删除结算。 */
    private WageSettlementEntity requireSettlementForUpdate(Long id) {
        return settlementRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(WageErrorCodeConstants.SETTLEMENT_NOT_EXISTS));
    }

    /** 校验客户端预期版本。 */
    private void validateVersion(WageSettlementEntity settlement, Integer expectedVersion) {
        if (!Objects.equals(settlement.getVersion(), expectedVersion)) {
            throw new ServiceException(WageErrorCodeConstants.SETTLEMENT_CONCURRENT_MODIFICATION);
        }
    }

    /** 校验结算处于任一允许状态。 */
    private void requireStatus(WageSettlementEntity settlement, WageSettlementStatusEnum... statuses) {
        for (WageSettlementStatusEnum status : statuses) {
            if (status.getStatus().equals(settlement.getSettlementStatus())) {
                return;
            }
        }
        throw new ServiceException(WageErrorCodeConstants.SETTLEMENT_STATUS_INVALID);
    }

    /** 校验最长 31 天结算周期。 */
    private void validateSettlementPeriod(LocalDate start, LocalDate end) {
        if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) >= MAX_PERIOD_DAYS) {
            throw new ServiceException(WageErrorCodeConstants.SETTLEMENT_PERIOD_INVALID);
        }
    }

    /** 校验指定员工均存在、启用。 */
    private void validateEmployeeScope(List<Long> employeeIds) {
        if (employeeIds.isEmpty()) {
            return;
        }
        List<UserEntity> users = userRepository.findByIdInAndDeletedFalse(employeeIds);
        if (users.size() != employeeIds.size()
                || users.stream().anyMatch(user -> !CommonStatusEnum.ENABLED.getStatus().equals(user.getStatus()))) {
            throw new ServiceException(WageErrorCodeConstants.WORK_RECORD_REFERENCE_INVALID,
                    "结算员工范围包含不存在、停用或已删除用户");
        }
    }

    /** 序列化员工范围；空范围使用 null 表示全部员工。 */
    private String writeScope(List<Long> employeeIds) {
        if (employeeIds.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(employeeIds);
        } catch (JacksonException exception) {
            throw new ServiceException(GlobalErrorCodeConstants.SYSTEM_ERROR, "结算员工范围序列化失败");
        }
    }

    /** 反序列化员工范围。 */
    private List<Long> readScope(String scope) {
        if (!StringUtils.hasText(scope)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(scope, new TypeReference<List<Long>>() { });
        } catch (JacksonException exception) {
            throw new ServiceException(GlobalErrorCodeConstants.SYSTEM_ERROR, "结算员工范围解析失败");
        }
    }

    /** 去重并按主键升序规范化范围。 */
    private List<Long> normalizeIds(List<Long> ids) {
        return ids == null ? Collections.emptyList()
                : ids.stream().distinct().sorted().toList();
    }

    /** 空范围查询时提供不会命中的占位主键，避免生成 IN ()。 */
    private List<Long> queryIds(List<Long> ids) {
        return ids.isEmpty() ? List.of(-1L) : ids;
    }

    /** 生成短且高熵的结算批次号，数据库唯一索引最终兜底。 */
    private String generateSettlementNo() {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return "WG" + LocalDate.now().toString().replace("-", "") + random;
    }

    /** 构造员工汇总响应。 */
    private EmployeeWageSummaryRespVO toEmployeeSummary(EmployeeWageSummaryProjection summary,
                                                         Map<Long, UserEntity> users) {
        EmployeeWageSummaryRespVO result = new EmployeeWageSummaryRespVO();
        result.setEmployeeId(summary.getEmployeeId());
        UserEntity user = users.get(summary.getEmployeeId());
        if (user != null) {
            result.setEmployeeNo(user.getUserNo());
            result.setEmployeeName(user.getUserName());
        }
        result.setQualifiedQuantity(summary.getQualifiedQuantity());
        result.setDefectQuantity(summary.getDefectQuantity());
        result.setTotalAmount(WageAmountUtils.fromAmountBasis(summary.getAmountBasis()));
        return result;
    }

    /** 构造工序汇总响应。 */
    private ProcessWageSummaryRespVO toProcessSummary(ProcessWageSummaryProjection summary,
                                                       Map<Long, CraftProcessEntity> processes) {
        ProcessWageSummaryRespVO result = new ProcessWageSummaryRespVO();
        result.setProcessId(summary.getProcessId());
        CraftProcessEntity process = processes.get(summary.getProcessId());
        if (process != null) {
            result.setProcessCode(process.getProcessCode());
            result.setProcessName(process.getProcessName());
        }
        result.setQualifiedQuantity(summary.getQualifiedQuantity());
        result.setDefectQuantity(summary.getDefectQuantity());
        result.setTotalAmount(WageAmountUtils.fromAmountBasis(summary.getAmountBasis()));
        return result;
    }

    /** 校验汇总日期顺序和最长一个日历年的查询范围。 */
    private void validateSummaryPeriod(WageSummaryReqVO reqVO) {
        if (reqVO.getPeriodEnd().isBefore(reqVO.getPeriodStart())) {
            throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR, "统计结束日期不能早于开始日期");
        }
        LocalDate latestAllowedEnd = reqVO.getPeriodStart().plusYears(1).minusDays(1);
        if (reqVO.getPeriodEnd().isAfter(latestAllowedEnd)) {
            throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR, "工资汇总周期最长一个日历年");
        }
    }

    /** 校验工资汇总分组数量未超过接口保护上限。 */
    private void validateSummaryResultCount(int resultCount) {
        if (resultCount > MAX_SUMMARY_RESULT_COUNT) {
            throw new ServiceException(WageErrorCodeConstants.SUMMARY_RESULT_LIMIT_EXCEEDED);
        }
    }

    /** 校验结算数量合计可写入 decimal(18,4) 字段。 */
    private void validateSettlementQuantity(BigDecimal quantity) {
        if (quantity.signum() < 0 || quantity.compareTo(MAX_SETTLEMENT_QUANTITY) > 0) {
            throw new ServiceException(WageErrorCodeConstants.SETTLEMENT_QUANTITY_OUT_OF_RANGE);
        }
    }

    /** 校验分页日期顺序。 */
    private void validateOptionalPagePeriod(WageSettlementPageReqVO reqVO) {
        if (reqVO.getPeriodStartBegin() != null && reqVO.getPeriodEndEnd() != null
                && reqVO.getPeriodEndEnd().isBefore(reqVO.getPeriodStartBegin())) {
            throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR, "结算周期结束上限不能早于开始下限");
        }
    }

    /** 规范化页码。 */
    private int normalizePageNo(int requested, int pageSize, long total) {
        return Math.min(requested, (int) ((total + pageSize - 1) / pageSize));
    }

    /** 空白字符串转 null。 */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
