package com.badminton.mes.module.production.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.DispatchAdjustLogRespVO;
import com.badminton.mes.module.production.controller.vo.DispatchPageReqVO;
import com.badminton.mes.module.production.controller.vo.DispatchRespVO;
import com.badminton.mes.module.production.controller.vo.DispatchSaveReqVO;
import com.badminton.mes.module.production.controller.vo.DispatchSuggestRespVO;
import com.badminton.mes.module.production.convert.DispatchOrderConvert;
import com.badminton.mes.module.production.dal.entity.DispatchAdjustLogEntity;
import com.badminton.mes.module.production.dal.entity.DispatchOrderEntity;
import com.badminton.mes.module.production.dal.entity.FactoryCalendarEntity;
import com.badminton.mes.module.production.dal.entity.ProductionLineEntity;
import com.badminton.mes.module.production.dal.entity.ShiftEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.redis.DispatchNoSequence;
import com.badminton.mes.module.production.dal.redis.WorkOrderCache;
import com.badminton.mes.module.production.dal.repository.DispatchAdjustLogRepository;
import com.badminton.mes.module.production.dal.repository.DispatchOrderRepository;
import com.badminton.mes.module.production.dal.repository.DispatchOrderSpecifications;
import com.badminton.mes.module.production.dal.repository.FactoryCalendarRepository;
import com.badminton.mes.module.production.dal.repository.ProductionLineRepository;
import com.badminton.mes.module.production.dal.repository.ShiftRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.enums.DispatchAdjustTypeEnum;
import com.badminton.mes.module.production.enums.DispatchStatusEnum;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;
import com.badminton.mes.module.production.service.DispatchOrderService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 派工单 Service 实现。
 *
 * <p>防超派采用业务 SQL 1.4 的双保险：事务内 SELECT ... FOR UPDATE 锁工单行
 * 做应用层校验，再以带 WHERE 上限条件的 UPDATE 兜底，影响行数为 0 即回滚。
 * 写路径锁序统一"工单行 → 派工单行"，且锁内重读派工单为差量与回退基线，
 * 避免交叉加锁死锁与过期快照腐蚀账目。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Service
public class DispatchOrderServiceImpl implements DispatchOrderService {

    private static final Logger logger = LoggerFactory.getLogger(DispatchOrderServiceImpl.class);

    /** 允许派工的工单状态：已下达、生产中 */
    private static final Set<Integer> DISPATCHABLE_STATUSES = Set.of(
            WorkOrderStatusEnum.RELEASED.getStatus(), WorkOrderStatusEnum.IN_PRODUCTION.getStatus());

    /** 允许取消的派工状态：待审核、已审核、已下发 */
    private static final Set<Integer> CANCELLABLE_STATUSES = Set.of(
            DispatchStatusEnum.PENDING_AUDIT.getStatus(), DispatchStatusEnum.AUDITED.getStatus(),
            DispatchStatusEnum.ISSUED.getStatus());

    /** 排产建议最多向后看的天数，防交期异常长时全表扫日历 */
    private static final int SUGGEST_MAX_DAYS = 60;

    private final DispatchOrderRepository dispatchOrderRepository;

    private final DispatchAdjustLogRepository dispatchAdjustLogRepository;

    private final WorkOrderRepository workOrderRepository;

    private final ProductionLineRepository productionLineRepository;

    private final ShiftRepository shiftRepository;

    private final FactoryCalendarRepository factoryCalendarRepository;

    private final DispatchNoSequence dispatchNoSequence;

    private final WorkOrderCache workOrderCache;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param dispatchOrderRepository     派工单 Repository
     * @param dispatchAdjustLogRepository 调整日志 Repository
     * @param workOrderRepository         工单 Repository
     * @param productionLineRepository    产线 Repository
     * @param shiftRepository             班次 Repository
     * @param factoryCalendarRepository   工厂日历 Repository
     * @param dispatchNoSequence          派工单号生成器
     * @param workOrderCache              工单详情缓存
     */
    public DispatchOrderServiceImpl(DispatchOrderRepository dispatchOrderRepository,
                                    DispatchAdjustLogRepository dispatchAdjustLogRepository,
                                    WorkOrderRepository workOrderRepository,
                                    ProductionLineRepository productionLineRepository,
                                    ShiftRepository shiftRepository,
                                    FactoryCalendarRepository factoryCalendarRepository,
                                    DispatchNoSequence dispatchNoSequence,
                                    WorkOrderCache workOrderCache) {
        this.dispatchOrderRepository = dispatchOrderRepository;
        this.dispatchAdjustLogRepository = dispatchAdjustLogRepository;
        this.workOrderRepository = workOrderRepository;
        this.productionLineRepository = productionLineRepository;
        this.shiftRepository = shiftRepository;
        this.factoryCalendarRepository = factoryCalendarRepository;
        this.dispatchNoSequence = dispatchNoSequence;
        this.workOrderCache = workOrderCache;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDispatch(DispatchSaveReqVO reqVO) {
        // 取号提前到锁外：Redis 抖动不拉长工单行锁持有时间；
        // 校验失败浪费一个号无害(单号仅要求唯一，允许空洞)
        String dispatchNo = dispatchNoSequence.nextNo();

        // 悲观锁锁工单行：两个计划员并发派同一工单时串行校验剩余可派(业务 SQL 1.4)
        WorkOrderEntity workOrder = workOrderRepository.findByIdForUpdate(reqVO.getWorkOrderId())
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_NOT_EXISTS));
        if (!DISPATCHABLE_STATUSES.contains(workOrder.getOrderStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.DISPATCH_WORK_ORDER_STATUS_INVALID);
        }
        if (reqVO.getPlanQuantity() > remainingDispatchable(workOrder)) {
            throw new ServiceException(ProductionErrorCodeConstants.DISPATCH_QUANTITY_EXCEED);
        }
        ProductionLineEntity line = validateLine(reqVO.getLineId());
        validateShift(reqVO.getShiftId());
        validatePlanTime(reqVO);
        validateWorkday(line.getWorkshopId(), reqVO.getPlanDate());
        validateCapacity(line, reqVO.getShiftId(), reqVO.getPlanDate(), reqVO.getPlanQuantity(), null);

        DispatchOrderEntity dispatch = new DispatchOrderEntity();
        dispatch.setDispatchNo(dispatchNo);
        dispatch.setWorkOrderId(reqVO.getWorkOrderId());
        dispatch.setLineId(reqVO.getLineId());
        dispatch.setShiftId(reqVO.getShiftId());
        dispatch.setPlanDate(reqVO.getPlanDate());
        dispatch.setPlanQuantity(reqVO.getPlanQuantity());
        dispatch.setPlanStartTime(reqVO.getPlanStartTime());
        dispatch.setPlanEndTime(reqVO.getPlanEndTime());
        dispatch.setSuggest(Boolean.TRUE.equals(reqVO.getSuggest()) ? 1 : 0);
        dispatch.setDispatchStatus(DispatchStatusEnum.PENDING_AUDIT.getStatus());
        dispatch.setCreateBy(SecurityContextHolder.getRequiredLoginUserId());
        dispatchOrderRepository.save(dispatch);

        // WHERE 上限条件兜底超派：0 行说明并发者已占满剩余量，整体回滚
        int updated = workOrderRepository.increaseDispatchedQuantity(
                reqVO.getWorkOrderId(), reqVO.getPlanQuantity());
        if (updated == 0) {
            throw new ServiceException(ProductionErrorCodeConstants.DISPATCH_QUANTITY_EXCEED);
        }

        DispatchAdjustTypeEnum createType = Boolean.TRUE.equals(reqVO.getSuggest())
                ? DispatchAdjustTypeEnum.SUGGEST_CREATE : DispatchAdjustTypeEnum.MANUAL_CREATE;
        saveAdjustLog(dispatch.getId(), createType, null, snapshot(dispatch), null);
        workOrderCache.evictAfterCommit(reqVO.getWorkOrderId());
        logger.info("[创建派工单] dispatchNo: {}, workOrderId: {}, quantity: {}",
                dispatch.getDispatchNo(), reqVO.getWorkOrderId(), reqVO.getPlanQuantity());
        return dispatch.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispatchSuggestRespVO> suggestDispatch(Long workOrderId) {
        WorkOrderEntity workOrder = workOrderRepository.findByIdAndDeletedFalse(workOrderId)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_NOT_EXISTS));
        if (!DISPATCHABLE_STATUSES.contains(workOrder.getOrderStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.DISPATCH_WORK_ORDER_STATUS_INVALID);
        }
        int remaining = remainingDispatchable(workOrder);
        if (remaining <= 0) {
            return List.of();
        }
        List<ProductionLineEntity> lines = productionLineRepository
                .findByWorkshopIdAndStatusAndDeletedFalseOrderByIdAsc(
                        workOrder.getWorkshopId(), CommonStatusEnum.ENABLED.getStatus());
        List<ShiftEntity> shifts = shiftRepository
                .findByStatusAndDeletedFalseOrderByIdAsc(CommonStatusEnum.ENABLED.getStatus());
        if (lines.isEmpty() || shifts.isEmpty()) {
            return List.of();
        }

        // 候选日期：max(今日, 计划开始) ~ min(交期, 今日+60)，工厂日历过滤非工作日
        LocalDate startDate = LocalDate.now().isAfter(workOrder.getPlanStartTime().toLocalDate())
                ? LocalDate.now() : workOrder.getPlanStartTime().toLocalDate();
        LocalDate endDate = workOrder.getPlanEndTime().toLocalDate();
        if (endDate.isAfter(startDate.plusDays(SUGGEST_MAX_DAYS))) {
            endDate = startDate.plusDays(SUGGEST_MAX_DAYS);
        }
        if (endDate.isBefore(startDate)) {
            return List.of();
        }
        Map<LocalDate, Integer> workdayMap = factoryCalendarRepository
                .findByWorkshopIdAndCalendarDateBetweenAndDeletedFalse(
                        workOrder.getWorkshopId(), startDate, endDate)
                .stream()
                .collect(Collectors.toMap(FactoryCalendarEntity::getCalendarDate,
                        FactoryCalendarEntity::getWorkday, (first, second) -> first));

        // 日期升序贪心填充：交期优先，一格填满再看下一格
        List<DispatchSuggestRespVO> suggestions = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate) && remaining > 0; date = date.plusDays(1)) {
            // 无日历记录按工作日处理(简化约定)
            if (Integer.valueOf(0).equals(workdayMap.get(date))) {
                continue;
            }
            for (ProductionLineEntity line : lines) {
                for (ShiftEntity shift : shifts) {
                    if (remaining <= 0) {
                        break;
                    }
                    int cellRemaining = cellRemainingCapacity(line, shift, shifts.size(), date, remaining);
                    if (cellRemaining <= 0) {
                        continue;
                    }
                    int quantity = Math.min(cellRemaining, remaining);
                    suggestions.add(buildSuggestion(workOrder, line, shift, date, quantity));
                    remaining -= quantity;
                }
            }
        }
        // 需求：建议须携带能否按期排完标记(wiki/16 §3.3)，格子用尽仍有缺口时置 false
        boolean canFinishOnTime = remaining <= 0;
        suggestions.forEach(suggestion -> suggestion.setCanFinishOnTime(canFinishOnTime));
        return suggestions;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DispatchRespVO> getDispatchPage(DispatchPageReqVO reqVO) {
        Specification<DispatchOrderEntity> specification = DispatchOrderSpecifications.page(reqVO);
        // 先 count：总数为 0 直接返回空页，省一次列表查询(SQL-005)
        long total = dispatchOrderRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }
        // 请求页码超过总页数时按最后一页返回(API-009)
        int pageSize = reqVO.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(reqVO.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<DispatchOrderEntity> page = dispatchOrderRepository.findAll(specification, pageRequest);
        return PageResult.of(toRespVOList(page.getContent()), total, pageNo, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public DispatchRespVO getDispatch(Long id) {
        DispatchOrderEntity dispatch = requireDispatch(id);
        return toRespVOList(List.of(dispatch)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDispatch(Long id, DispatchSaveReqVO reqVO) {
        // 标量查询取所属工单 id：不把派工单实体提前载入一级缓存，
        // 保证下方锁内首次加载读到的是最新已提交状态
        Long workOrderId = dispatchOrderRepository.findWorkOrderIdById(id)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.DISPATCH_ORDER_NOT_EXISTS));

        // 与创建同序先锁工单行再锁派工单行，避免交叉加锁死锁
        WorkOrderEntity workOrder = workOrderRepository.findByIdForUpdate(workOrderId)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_NOT_EXISTS));
        // 已完工/已作废工单的存量派工单不允许继续调整(与创建口径一致)
        if (!DISPATCHABLE_STATUSES.contains(workOrder.getOrderStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.DISPATCH_WORK_ORDER_STATUS_INVALID);
        }

        // 锁内首载派工单：状态校验与差量基线均取最新值
        DispatchOrderEntity dispatch = dispatchOrderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.DISPATCH_ORDER_NOT_EXISTS));
        boolean issued = DispatchStatusEnum.ISSUED.getStatus().equals(dispatch.getDispatchStatus());
        boolean editable = DispatchStatusEnum.PENDING_AUDIT.getStatus().equals(dispatch.getDispatchStatus())
                || DispatchStatusEnum.AUDITED.getStatus().equals(dispatch.getDispatchStatus())
                || issued;
        if (!editable) {
            throw new ServiceException(ProductionErrorCodeConstants.DISPATCH_STATUS_NOT_ALLOW_UPDATE);
        }
        // 需求：下发后调整必须记录原因
        if (issued && !StringUtils.hasText(reqVO.getAdjustReason())) {
            throw new ServiceException(ProductionErrorCodeConstants.DISPATCH_ADJUST_REASON_REQUIRED);
        }

        int quantityDelta = reqVO.getPlanQuantity() - dispatch.getPlanQuantity();
        if (quantityDelta > remainingDispatchable(workOrder)) {
            throw new ServiceException(ProductionErrorCodeConstants.DISPATCH_QUANTITY_EXCEED);
        }
        ProductionLineEntity line = validateLine(reqVO.getLineId());
        validateShift(reqVO.getShiftId());
        validatePlanTime(reqVO);
        validateWorkday(line.getWorkshopId(), reqVO.getPlanDate());
        // 产能校验排除自身旧占用
        validateCapacity(line, reqVO.getShiftId(), reqVO.getPlanDate(), reqVO.getPlanQuantity(), id);

        String before = snapshot(dispatch);
        dispatch.setLineId(reqVO.getLineId());
        dispatch.setShiftId(reqVO.getShiftId());
        dispatch.setPlanDate(reqVO.getPlanDate());
        dispatch.setPlanQuantity(reqVO.getPlanQuantity());
        dispatch.setPlanStartTime(reqVO.getPlanStartTime());
        dispatch.setPlanEndTime(reqVO.getPlanEndTime());
        if (issued) {
            dispatch.setAdjustReason(reqVO.getAdjustReason());
        }
        dispatchOrderRepository.save(dispatch);

        if (quantityDelta > 0) {
            int updated = workOrderRepository.increaseDispatchedQuantity(workOrderId, quantityDelta);
            if (updated == 0) {
                throw new ServiceException(ProductionErrorCodeConstants.DISPATCH_QUANTITY_EXCEED);
            }
        } else if (quantityDelta < 0) {
            int decreased = workOrderRepository.decreaseDispatchedQuantity(workOrderId, -quantityDelta);
            if (decreased == 0) {
                logger.error("[派工缩量回退失败] dispatchId: {}, workOrderId: {}, delta: {}，已派数量将回负，人工核查",
                        id, workOrderId, quantityDelta);
                throw new ServiceException(GlobalErrorCodeConstants.SYSTEM_ERROR,
                        "调整回退失败，工单已派数量异常，请联系管理员核查");
            }
        }
        saveAdjustLog(id, DispatchAdjustTypeEnum.ADJUST, before, snapshot(dispatch), reqVO.getAdjustReason());
        if (quantityDelta != 0) {
            workOrderCache.evictAfterCommit(workOrderId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditDispatch(Long id) {
        DispatchOrderEntity dispatch = requireDispatch(id);
        int updated = dispatchOrderRepository.updateToAudited(id,
                DispatchStatusEnum.PENDING_AUDIT.getStatus(), DispatchStatusEnum.AUDITED.getStatus(),
                SecurityContextHolder.getRequiredLoginUserId(), LocalDateTime.now());
        if (updated == 0) {
            throw new ServiceException(ProductionErrorCodeConstants.DISPATCH_STATUS_NOT_ALLOW_AUDIT);
        }
        saveAdjustLog(id, DispatchAdjustTypeEnum.AUDIT, null, snapshot(dispatch), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void issueDispatch(Long id) {
        DispatchOrderEntity dispatch = requireDispatch(id);
        int updated = dispatchOrderRepository.updateStatus(id,
                DispatchStatusEnum.AUDITED.getStatus(), DispatchStatusEnum.ISSUED.getStatus());
        if (updated == 0) {
            throw new ServiceException(ProductionErrorCodeConstants.DISPATCH_STATUS_NOT_ALLOW_ISSUE);
        }
        saveAdjustLog(id, DispatchAdjustTypeEnum.ISSUE, null, snapshot(dispatch), null);
        logger.info("[下发派工单] dispatchNo: {}", dispatch.getDispatchNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelDispatch(Long id, String reason) {
        Long workOrderId = dispatchOrderRepository.findWorkOrderIdById(id)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.DISPATCH_ORDER_NOT_EXISTS));
        workOrderRepository.findByIdForUpdate(workOrderId)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_NOT_EXISTS));
        DispatchOrderEntity dispatch = dispatchOrderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.DISPATCH_ORDER_NOT_EXISTS));
        if (!CANCELLABLE_STATUSES.contains(dispatch.getDispatchStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.DISPATCH_STATUS_NOT_ALLOW_CANCEL);
        }
        // CAS 保留：防未来现场模块等不加锁写方并发改状态(3 执行中/4 已完成)
        int updated = dispatchOrderRepository.updateStatus(id,
                dispatch.getDispatchStatus(), DispatchStatusEnum.CANCELLED.getStatus());
        if (updated == 0) {
            // 读取与 CAS 之间状态被并发改变(如现场已回写执行中)
            throw new ServiceException(ProductionErrorCodeConstants.DISPATCH_STATUS_NOT_ALLOW_CANCEL);
        }
        int decreased = workOrderRepository.decreaseDispatchedQuantity(
                workOrderId, dispatch.getPlanQuantity());
        if (decreased == 0) {
            logger.error("[取消派工回退失败] dispatchId: {}, workOrderId: {}, quantity: {}，已派数量将回负，人工核查",
                    id, workOrderId, dispatch.getPlanQuantity());
            throw new ServiceException(GlobalErrorCodeConstants.SYSTEM_ERROR,
                    "取消回退失败，工单已派数量异常，请联系管理员核查");
        }
        saveAdjustLog(id, DispatchAdjustTypeEnum.CANCEL, snapshot(dispatch), null, reason);
        workOrderCache.evictAfterCommit(workOrderId);
        logger.info("[取消派工单] dispatchNo: {}, reason: {}", dispatch.getDispatchNo(), reason);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispatchAdjustLogRespVO> getAdjustLogs(Long id) {
        requireDispatch(id);
        return DispatchOrderConvert.toAdjustLogRespVOList(
                dispatchAdjustLogRepository.findByDispatchOrderIdAndDeletedFalseOrderByIdDesc(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispatchRespVO> getLineSchedule(Long lineId, LocalDate startDate, LocalDate endDate) {
        List<DispatchOrderEntity> dispatches = dispatchOrderRepository.findLineSchedule(
                lineId, startDate, endDate, DispatchStatusEnum.CANCELLED.getStatus());
        return toRespVOList(dispatches);
    }

    /**
     * 工单剩余可派数量 = FLOOR(计划数量×(1+超产比例/100)) − 已派数量。
     *
     * @param workOrder 工单实体
     * @return 剩余可派数量
     */
    private int remainingDispatchable(WorkOrderEntity workOrder) {
        BigDecimal overRatio = workOrder.getOverRatio() == null ? BigDecimal.ZERO : workOrder.getOverRatio();
        int upperLimit = BigDecimal.valueOf(workOrder.getPlanQuantity())
                .multiply(BigDecimal.ONE.add(overRatio.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)))
                .setScale(0, RoundingMode.FLOOR)
                .intValue();
        int dispatched = workOrder.getDispatchedQuantity() == null ? 0 : workOrder.getDispatchedQuantity();
        return upperLimit - dispatched;
    }

    /**
     * 校验产线存在且启用。
     *
     * @param lineId 产线主键
     * @return 产线实体
     */
    private ProductionLineEntity validateLine(Long lineId) {
        ProductionLineEntity line = productionLineRepository.findByIdAndDeletedFalseForUpdate(lineId)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.DISPATCH_LINE_NOT_AVAILABLE));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(line.getStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.DISPATCH_LINE_NOT_AVAILABLE);
        }
        return line;
    }

    /**
     * 校验班次存在且启用。
     *
     * @param shiftId 班次主键
     */
    private void validateShift(Long shiftId) {
        ShiftEntity shift = shiftRepository.findByIdAndDeletedFalse(shiftId)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.DISPATCH_SHIFT_NOT_AVAILABLE));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(shift.getStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.DISPATCH_SHIFT_NOT_AVAILABLE);
        }
    }

    /**
     * 校验派工计划时间：结束必须晚于开始。
     *
     * @param reqVO 派工请求
     */
    private void validatePlanTime(DispatchSaveReqVO reqVO) {
        if (!reqVO.getPlanEndTime().isAfter(reqVO.getPlanStartTime())) {
            throw new ServiceException(ProductionErrorCodeConstants.DISPATCH_PLAN_TIME_INVALID);
        }
    }

    /**
     * 校验排产日期为工作日；无日历记录按工作日处理(简化约定)。
     *
     * @param workshopId 车间主键(取自产线)
     * @param planDate   排产日期
     */
    private void validateWorkday(Long workshopId, LocalDate planDate) {
        factoryCalendarRepository.findByWorkshopIdAndCalendarDateAndDeletedFalse(workshopId, planDate)
                .ifPresent(calendar -> {
                    if (Integer.valueOf(0).equals(calendar.getWorkday())) {
                        throw new ServiceException(ProductionErrorCodeConstants.DISPATCH_DATE_NOT_WORKDAY);
                    }
                });
    }

    /**
     * 产能校验：同产线同日同班次累计排产(排除已取消与自身) + 本次 ≤ 班次产能。
     * 班次产能 = 标准日产能 ÷ 启用班次数(均摊简化)；日产能未维护则跳过校验。
     *
     * <p>持有的工单行锁无法阻止其他工单向同一产线并发排产，存在轻微超产能竞态，
     * 本期接受(严格方案需锁产线行，见设计文档"不做/延后")。
     *
     * @param line      产线实体
     * @param shiftId   班次主键
     * @param planDate  排产日期
     * @param quantity  本次派工数量
     * @param excludeId 调整场景排除的派工单自身，可空
     */
    private void validateCapacity(ProductionLineEntity line, Long shiftId, LocalDate planDate,
                                  Integer quantity, Long excludeId) {
        if (line.getStandardCapacity() == null) {
            return;
        }
        int shiftCount = Math.max(1, shiftRepository
                .findByStatusAndDeletedFalseOrderByIdAsc(CommonStatusEnum.ENABLED.getStatus()).size());
        long shiftCapacity = line.getStandardCapacity() / shiftCount;
        long planned = dispatchOrderRepository.sumPlannedQuantity(line.getId(), planDate, shiftId,
                DispatchStatusEnum.CANCELLED.getStatus(), excludeId);
        if (planned + quantity > shiftCapacity) {
            throw new ServiceException(ProductionErrorCodeConstants.DISPATCH_CAPACITY_EXCEED);
        }
    }

    /**
     * 单格(产线×班次×日期)剩余产能，排产建议用。
     *
     * @param line          产线实体
     * @param shift         班次实体
     * @param shiftCount    启用班次数
     * @param date          日期
     * @param unboundedFill 产能未维护时的建议填充量(取剩余可派)
     * @return 剩余产能
     */
    private int cellRemainingCapacity(ProductionLineEntity line, ShiftEntity shift,
                                      int shiftCount, LocalDate date, int unboundedFill) {
        long planned = dispatchOrderRepository.sumPlannedQuantity(line.getId(), date, shift.getId(),
                DispatchStatusEnum.CANCELLED.getStatus(), null);
        if (line.getStandardCapacity() == null) {
            // 产能未维护：无法约束，仅在无已排记录的格子建议一次性排完
            return planned > 0 ? 0 : unboundedFill;
        }
        long shiftCapacity = line.getStandardCapacity() / Math.max(1, shiftCount);
        return (int) Math.max(0, shiftCapacity - planned);
    }

    /**
     * 组装排产建议行。
     *
     * @param workOrder 工单实体
     * @param line      产线实体
     * @param shift     班次实体
     * @param date      建议日期
     * @param quantity  建议数量
     * @return 建议行
     */
    private DispatchSuggestRespVO buildSuggestion(WorkOrderEntity workOrder, ProductionLineEntity line,
                                                  ShiftEntity shift, LocalDate date, int quantity) {
        DispatchSuggestRespVO suggestion = new DispatchSuggestRespVO();
        suggestion.setWorkOrderId(workOrder.getId());
        suggestion.setKitStatus(workOrder.getKitStatus());
        suggestion.setLineId(line.getId());
        suggestion.setLineName(line.getLineName());
        suggestion.setShiftId(shift.getId());
        suggestion.setShiftName(shift.getShiftName());
        suggestion.setPlanDate(date);
        suggestion.setPlanQuantity(quantity);
        suggestion.setPlanStartTime(date.atTime(shift.getStartTime()));
        // 夜班等跨天班次：结束时间不晚于开始时间则结束日期 +1
        LocalDate endDate = shift.getEndTime().isAfter(shift.getStartTime()) ? date : date.plusDays(1);
        suggestion.setPlanEndTime(endDate.atTime(shift.getEndTime()));
        return suggestion;
    }

    /**
     * 按主键查询未删除派工单，不存在抛错。
     *
     * @param id 派工单主键
     * @return 派工单实体
     */
    private DispatchOrderEntity requireDispatch(Long id) {
        return dispatchOrderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.DISPATCH_ORDER_NOT_EXISTS));
    }

    /**
     * 派工单排产要素快照(产线/班次/日期/数量)，手工拼 JSON 保持字段紧凑稳定。
     *
     * @param dispatch 派工单实体
     * @return JSON 快照串
     */
    private String snapshot(DispatchOrderEntity dispatch) {
        return String.format(
                "{\"lineId\":%d,\"shiftId\":%d,\"planDate\":\"%s\",\"planQuantity\":%d,"
                        + "\"planStartTime\":\"%s\",\"planEndTime\":\"%s\"}",
                dispatch.getLineId(), dispatch.getShiftId(), dispatch.getPlanDate(),
                dispatch.getPlanQuantity(), dispatch.getPlanStartTime(), dispatch.getPlanEndTime());
    }

    /**
     * 写排产调整日志。
     *
     * @param dispatchOrderId 派工单主键
     * @param type            记录类型
     * @param before          调整前快照，可空
     * @param after           调整后快照，可空
     * @param reason          调整原因，可空
     */
    private void saveAdjustLog(Long dispatchOrderId, DispatchAdjustTypeEnum type,
                               String before, String after, String reason) {
        DispatchAdjustLogEntity log = new DispatchAdjustLogEntity();
        log.setDispatchOrderId(dispatchOrderId);
        log.setAdjustType(type.getType());
        log.setBeforeSnapshot(before);
        log.setAfterSnapshot(after);
        log.setAdjustReason(reason);
        log.setOperatorId(SecurityContextHolder.getRequiredLoginUserId());
        dispatchAdjustLogRepository.save(log);
    }

    /**
     * 实体列表转响应 VO 并批量回填工单号/产品名/齐套状态/产线名/班次名，避免 N+1。
     *
     * @param dispatches 派工单实体列表
     * @return 响应 VO 列表
     */
    private List<DispatchRespVO> toRespVOList(List<DispatchOrderEntity> dispatches) {
        if (dispatches.isEmpty()) {
            return List.of();
        }
        Map<Long, WorkOrderEntity> workOrderMap = workOrderRepository
                .findByIdInAndDeletedFalse(dispatches.stream()
                        .map(DispatchOrderEntity::getWorkOrderId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(WorkOrderEntity::getId, Function.identity(), (first, second) -> first));
        Map<Long, ProductionLineEntity> lineMap = productionLineRepository
                .findAllById(dispatches.stream().map(DispatchOrderEntity::getLineId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(ProductionLineEntity::getId, Function.identity(),
                        (first, second) -> first));
        Map<Long, ShiftEntity> shiftMap = shiftRepository
                .findAllById(dispatches.stream().map(DispatchOrderEntity::getShiftId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(ShiftEntity::getId, Function.identity(), (first, second) -> first));
        return dispatches.stream().map(dispatch -> {
            DispatchRespVO respVO = DispatchOrderConvert.toRespVO(dispatch);
            WorkOrderEntity workOrder = workOrderMap.get(dispatch.getWorkOrderId());
            if (workOrder != null) {
                respVO.setWorkOrderNo(workOrder.getWorkOrderNo());
                respVO.setProductName(workOrder.getProductName());
                respVO.setKitStatus(workOrder.getKitStatus());
            }
            ProductionLineEntity line = lineMap.get(dispatch.getLineId());
            if (line != null) {
                respVO.setLineName(line.getLineName());
            }
            ShiftEntity shift = shiftMap.get(dispatch.getShiftId());
            if (shift != null) {
                respVO.setShiftName(shift.getShiftName());
            }
            return respVO;
        }).toList();
    }
}
