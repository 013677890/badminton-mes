package com.badminton.mes.module.production.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.WorkOrderMaterialRespVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderPageReqVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderRespVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderSaveReqVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderStatusLogRespVO;
import com.badminton.mes.module.production.convert.WorkOrderConvert;
import com.badminton.mes.module.production.dal.entity.BomDetailEntity;
import com.badminton.mes.module.production.dal.entity.BomEntity;
import com.badminton.mes.module.production.dal.entity.MaterialEntity;
import com.badminton.mes.module.production.dal.entity.ProductEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderMaterialEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderStatusLogEntity;
import com.badminton.mes.module.production.dal.entity.WorkshopEntity;
import com.badminton.mes.module.production.dal.redis.WorkOrderCache;
import com.badminton.mes.module.production.dal.redis.WorkOrderNoSequence;
import com.badminton.mes.module.production.dal.repository.BomDetailRepository;
import com.badminton.mes.module.production.dal.repository.BomRepository;
import com.badminton.mes.module.production.dal.repository.MaterialRepository;
import com.badminton.mes.module.production.dal.repository.ProductRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderMaterialRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderSpecifications;
import com.badminton.mes.module.production.dal.repository.WorkOrderStatusLogRepository;
import com.badminton.mes.module.production.dal.repository.WorkshopRepository;
import com.badminton.mes.module.production.enums.BomStatusEnum;
import com.badminton.mes.module.production.enums.WorkOrderChangeTypeEnum;
import com.badminton.mes.module.production.enums.WorkOrderSourceTypeEnum;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;
import com.badminton.mes.module.production.service.WorkOrderService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 生产工单 Service 实现。
 *
 * <p>Service 负责编排业务校验、事务、数据库写入和缓存失效时机。
 * Redis Key、TTL、序列化和异常降级均下沉到 {@link WorkOrderCache}。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
@Service
public class WorkOrderServiceImpl implements WorkOrderService {

    private static final Logger logger = LoggerFactory.getLogger(WorkOrderServiceImpl.class);

    private final WorkOrderRepository workOrderRepository;

    private final ProductRepository productRepository;

    private final WorkshopRepository workshopRepository;

    private final BomRepository bomRepository;

    private final BomDetailRepository bomDetailRepository;

    private final MaterialRepository materialRepository;

    private final WorkOrderMaterialRepository workOrderMaterialRepository;

    private final WorkOrderStatusLogRepository workOrderStatusLogRepository;

    private final WorkOrderCache workOrderCache;

    private final WorkOrderNoSequence workOrderNoSequence;

    /**
     * 构造器注入：依赖不可变、便于单测中直接 new 出被测对象。
     *
     * @param workOrderRepository          工单 Repository
     * @param productRepository            产品 Repository
     * @param workshopRepository           车间 Repository
     * @param bomRepository                BOM Repository
     * @param bomDetailRepository          BOM 明细 Repository
     * @param materialRepository           物料 Repository
     * @param workOrderMaterialRepository  工单物料需求 Repository
     * @param workOrderStatusLogRepository 工单状态日志 Repository
     * @param workOrderCache               工单缓存组件
     * @param workOrderNoSequence          工单号流水生成器
     */
    public WorkOrderServiceImpl(WorkOrderRepository workOrderRepository, ProductRepository productRepository,
                                WorkshopRepository workshopRepository, BomRepository bomRepository,
                                BomDetailRepository bomDetailRepository, MaterialRepository materialRepository,
                                WorkOrderMaterialRepository workOrderMaterialRepository,
                                WorkOrderStatusLogRepository workOrderStatusLogRepository,
                                WorkOrderCache workOrderCache, WorkOrderNoSequence workOrderNoSequence) {
        this.workOrderRepository = workOrderRepository;
        this.productRepository = productRepository;
        this.workshopRepository = workshopRepository;
        this.bomRepository = bomRepository;
        this.bomDetailRepository = bomDetailRepository;
        this.materialRepository = materialRepository;
        this.workOrderMaterialRepository = workOrderMaterialRepository;
        this.workOrderStatusLogRepository = workOrderStatusLogRepository;
        this.workOrderCache = workOrderCache;
        this.workOrderNoSequence = workOrderNoSequence;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWorkOrder(WorkOrderSaveReqVO reqVO) {
        validatePlanTime(reqVO);
        ProductEntity product = validateProduct(reqVO.getProductId());
        validateWorkshop(reqVO.getWorkshopId());
        String workOrderNo = resolveWorkOrderNo(reqVO.getWorkOrderNo());

        WorkOrderEntity workOrder = WorkOrderConvert.toEntity(reqVO);
        workOrder.setWorkOrderNo(workOrderNo);
        // 冗余字段以产品档案为准回填，不信任前端提交，避免与档案不一致
        workOrder.setProductName(product.getProductName());
        workOrder.setSpec(product.getSpec());
        workOrder.setUnitId(product.getUnitId());
        workOrder.setSourceType(WorkOrderSourceTypeEnum.MANUAL.getType());
        workOrder.setOrderStatus(WorkOrderStatusEnum.CREATED.getStatus());
        workOrder.setCreateBy(SecurityContextHolder.getRequiredLoginUserId());
        try {
            workOrderRepository.saveAndFlush(workOrder);
        } catch (DataIntegrityViolationException e) {
            // 并发创建穿透应用层查重时，由唯一索引 uk_work_order_no 兜底，转成业务错误提示
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_NO_DUPLICATE);
        }

        logger.info("[创建工单] id: {}, workOrderNo: {}", workOrder.getId(), workOrderNo);
        return workOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkOrder(Long id, WorkOrderSaveReqVO reqVO) {
        validatePlanTime(reqVO);
        WorkOrderEntity existing = validateWorkOrderExists(id);
        if (WorkOrderStatusEnum.CREATED.getStatus().equals(existing.getOrderStatus())) {
            updateCreatedWorkOrder(id, reqVO);
            return;
        }
        if (WorkOrderStatusEnum.RELEASED.getStatus().equals(existing.getOrderStatus())) {
            updateReleasedWorkOrder(existing, reqVO);
            return;
        }

        // 生产中及之后的状态不允许直接修改计划
        throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_UPDATE);
    }

    /**
     * "已创建"状态修改：允许调整全部计划字段。
     *
     * @param id    工单主键
     * @param reqVO 修改请求
     */
    private void updateCreatedWorkOrder(Long id, WorkOrderSaveReqVO reqVO) {
        ProductEntity product = validateProduct(reqVO.getProductId());
        validateWorkshop(reqVO.getWorkshopId());

        WorkOrderEntity updateEntity = WorkOrderConvert.toEntity(reqVO);
        int rows = workOrderRepository.updatePlan(id, updateEntity.getProductId(), product.getProductName(),
                product.getSpec(), product.getUnitId(), updateEntity.getBatchNo(), updateEntity.getBomId(),
                updateEntity.getRoutingId(), updateEntity.getCustomerId(), updateEntity.getWorkshopId(),
                updateEntity.getPlanQuantity(), updateEntity.getOverRatio(), updateEntity.getPriority(),
                updateEntity.getPlanStartTime(), updateEntity.getPlanEndTime(),
                WorkOrderStatusEnum.CREATED.getStatus());
        // CAS 未命中：校验与更新的间隙内工单被并发下达或删除
        if (rows == 0) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_UPDATE);
        }

        // 先库后缓存：提交后再删缓存，避免提交前并发回源把旧值填回 Redis
        evictCacheAfterCommit(id);
    }

    /**
     * "已下达"状态修改：仅允许调整计划数量与计划时间，变更原因必填并记入状态日志
     * (需求规则：工单已下达后修改计划数量或交期应记录变更原因)。
     * 计划数量变化时按 BOM 重算物料需求，保证领料/齐套按新数量执行。
     *
     * @param existing 修改前工单数据
     * @param reqVO    修改请求
     */
    private void updateReleasedWorkOrder(WorkOrderEntity existing, WorkOrderSaveReqVO reqVO) {
        if (!StringUtils.hasText(reqVO.getChangeReason())) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_CHANGE_REASON_REQUIRED);
        }

        Long id = existing.getId();
        int rows = workOrderRepository.updateReleasedPlan(id, reqVO.getPlanQuantity(),
                reqVO.getPlanStartTime(), reqVO.getPlanEndTime(), WorkOrderStatusEnum.RELEASED.getStatus());
        if (rows == 0) {
            // CAS 条件含"计划数量不低于已派工数量"，先给出更精确的数量提示
            if (existing.getDispatchedQuantity() != null
                    && existing.getDispatchedQuantity() > reqVO.getPlanQuantity()) {
                throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_PLAN_LESS_THAN_DISPATCHED);
            }
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_UPDATE);
        }

        // 计划数量变化才重算物料需求；只改交期不触碰物料数据
        if (!reqVO.getPlanQuantity().equals(existing.getPlanQuantity())) {
            recalculateWorkOrderMaterials(id, existing.getBomId(), reqVO.getPlanQuantity());
        }
        insertStatusLog(id, WorkOrderStatusEnum.RELEASED.getStatus(), WorkOrderStatusEnum.RELEASED.getStatus(),
                WorkOrderChangeTypeEnum.PLAN_CHANGE, reqVO.getChangeReason());
        evictCacheAfterCommit(id);
        logger.info("[已下达工单计划变更] id: {}, planQuantity: {}, reason: {}", id, reqVO.getPlanQuantity(),
                reqVO.getChangeReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkOrder(Long id) {
        WorkOrderEntity existing = validateWorkOrderExists(id);
        if (!WorkOrderStatusEnum.CREATED.getStatus().equals(existing.getOrderStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_DELETE);
        }

        // 逻辑删除，JPQL 带状态条件构成 CAS，防并发下达后误删
        int rows = workOrderRepository.logicDeleteById(id, WorkOrderStatusEnum.CREATED.getStatus());
        if (rows == 0) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_DELETE);
        }

        evictCacheAfterCommit(id);
        logger.info("[删除工单] id: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseWorkOrder(Long id) {
        // 先 CAS 更新再对失败查因，避免"先查后改"竞态；条件含 BOM/工艺路线非空校验
        int rows = workOrderRepository.updateToReleased(id, WorkOrderStatusEnum.CREATED.getStatus(),
                WorkOrderStatusEnum.RELEASED.getStatus());
        if (rows == 1) {
            WorkOrderEntity workOrder = validateWorkOrderExists(id);
            // BOM 校验或明细为空抛异常时，本事务连同上面的状态更新一起回滚
            generateWorkOrderMaterials(workOrder);
            insertStatusLog(id, WorkOrderStatusEnum.CREATED.getStatus(), WorkOrderStatusEnum.RELEASED.getStatus(),
                    WorkOrderChangeTypeEnum.STATUS_TRANSITION, null);
            evictCacheAfterCommit(id);
            logger.info("[下达工单] id: {}", id);
            return;
        }

        // CAS 未命中，逐项查明原因给出精确提示(EXC-003 分门别类提示)
        WorkOrderEntity workOrder = validateWorkOrderExists(id);
        if (!WorkOrderStatusEnum.CREATED.getStatus().equals(workOrder.getOrderStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_RELEASE);
        }
        if (workOrder.getBomId() == null || workOrder.getRoutingId() == null) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_RELEASE_MISSING_BOM_ROUTING);
        }

        // 查因瞬间状态又被并发修改，按状态不允许下达处理
        throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_RELEASE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pauseWorkOrder(Long id, String reason) {
        // 逐个前置状态尝试 CAS：命中的分支即真实的变更前状态，
        // 避免"先读后改"窗口内状态漂移导致日志记错、恢复时还原到错误状态
        Integer fromStatus = transitionFromAny(id,
                List.of(WorkOrderStatusEnum.RELEASED.getStatus(), WorkOrderStatusEnum.IN_PRODUCTION.getStatus()),
                WorkOrderStatusEnum.PAUSED.getStatus());
        if (fromStatus == null) {
            validateWorkOrderExists(id);
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_PAUSE);
        }

        insertStatusLog(id, fromStatus, WorkOrderStatusEnum.PAUSED.getStatus(),
                WorkOrderChangeTypeEnum.STATUS_TRANSITION, reason);
        evictCacheAfterCommit(id);
        logger.info("[暂停工单] id: {}, fromStatus: {}, reason: {}", id, fromStatus, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resumeWorkOrder(Long id) {
        validateWorkOrderExists(id);
        // 恢复到暂停前状态：从最近一条"流转到暂停"的日志取 fromStatus；无日志时回到已下达
        Integer restoreStatus = workOrderStatusLogRepository
                .findFirstByWorkOrderIdAndToStatusAndDeletedFalseOrderByIdDesc(id,
                        WorkOrderStatusEnum.PAUSED.getStatus())
                .map(WorkOrderStatusLogEntity::getFromStatus)
                .orElse(WorkOrderStatusEnum.RELEASED.getStatus());
        int rows = workOrderRepository.updateStatus(id, List.of(WorkOrderStatusEnum.PAUSED.getStatus()),
                restoreStatus);
        if (rows == 0) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_RESUME);
        }

        insertStatusLog(id, WorkOrderStatusEnum.PAUSED.getStatus(), restoreStatus,
                WorkOrderChangeTypeEnum.STATUS_TRANSITION, null);
        evictCacheAfterCommit(id);
        logger.info("[恢复工单] id: {}, restoreStatus: {}", id, restoreStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishWorkOrder(Long id) {
        // 状态与完工数量上限在同一条 UPDATE 内原子校验(消除检查-执行竞态)；
        // 逐个前置状态尝试，命中分支即真实的变更前状态
        Integer fromStatus = null;
        for (Integer candidate : List.of(WorkOrderStatusEnum.RELEASED.getStatus(),
                WorkOrderStatusEnum.IN_PRODUCTION.getStatus())) {
            if (workOrderRepository.updateToFinished(id, candidate,
                    WorkOrderStatusEnum.FINISHED.getStatus()) == 1) {
                fromStatus = candidate;
                break;
            }
        }
        if (fromStatus == null) {
            // CAS 未命中，逐项查明原因给出精确提示(EXC-003 分门别类提示)
            WorkOrderEntity workOrder = validateWorkOrderExists(id);
            boolean statusAllowed = WorkOrderStatusEnum.RELEASED.getStatus().equals(workOrder.getOrderStatus())
                    || WorkOrderStatusEnum.IN_PRODUCTION.getStatus().equals(workOrder.getOrderStatus());
            if (!statusAllowed) {
                throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_FINISH);
            }
            validateFinishQuantityLimit(workOrder);
            // 查因瞬间状态又被并发修改，按状态不允许完工处理
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_FINISH);
        }

        insertStatusLog(id, fromStatus, WorkOrderStatusEnum.FINISHED.getStatus(),
                WorkOrderChangeTypeEnum.STATUS_TRANSITION, null);
        evictCacheAfterCommit(id);
        logger.info("[完工工单] id: {}, fromStatus: {}", id, fromStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeWorkOrder(Long id) {
        validateWorkOrderExists(id);
        int rows = workOrderRepository.updateStatus(id, List.of(WorkOrderStatusEnum.FINISHED.getStatus()),
                WorkOrderStatusEnum.CLOSED.getStatus());
        if (rows == 0) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_CLOSE);
        }

        insertStatusLog(id, WorkOrderStatusEnum.FINISHED.getStatus(), WorkOrderStatusEnum.CLOSED.getStatus(),
                WorkOrderChangeTypeEnum.STATUS_TRANSITION, null);
        evictCacheAfterCommit(id);
        logger.info("[关闭工单] id: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelWorkOrder(Long id, String reason) {
        // 逐个前置状态尝试 CAS，命中分支即真实的变更前状态
        Integer fromStatus = transitionFromAny(id,
                List.of(WorkOrderStatusEnum.CREATED.getStatus(), WorkOrderStatusEnum.RELEASED.getStatus()),
                WorkOrderStatusEnum.CANCELLED.getStatus());
        if (fromStatus == null) {
            validateWorkOrderExists(id);
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_CANCEL);
        }

        // 物料需求随单失效，避免齐套/领料按物料聚合时计入已作废工单
        workOrderMaterialRepository.logicDeleteByWorkOrderId(id);
        insertStatusLog(id, fromStatus, WorkOrderStatusEnum.CANCELLED.getStatus(),
                WorkOrderChangeTypeEnum.STATUS_TRANSITION, reason);
        evictCacheAfterCommit(id);
        logger.info("[作废工单] id: {}, fromStatus: {}, reason: {}", id, fromStatus, reason);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkOrderMaterialRespVO> getWorkOrderMaterials(Long id) {
        validateWorkOrderExists(id);
        List<WorkOrderMaterialEntity> list = workOrderMaterialRepository.findByWorkOrderIdAndDeletedFalse(id);
        if (list.isEmpty()) {
            return List.of();
        }

        // 单工单 BOM 明细规模有限，一次 in 查询回填物料编码/名称，避免 N+1
        List<Long> materialIds = list.stream().map(WorkOrderMaterialEntity::getMaterialId).toList();
        Map<Long, MaterialEntity> materialMap = materialRepository.findByIdInAndDeletedFalse(materialIds).stream()
                .collect(Collectors.toMap(MaterialEntity::getId, Function.identity(), (first, second) -> first));
        return WorkOrderConvert.toMaterialRespVOList(list, materialMap);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkOrderStatusLogRespVO> getWorkOrderStatusLogs(Long id) {
        validateWorkOrderExists(id);
        return WorkOrderConvert.toStatusLogRespVOList(
                workOrderStatusLogRepository.findByWorkOrderIdAndDeletedFalseOrderByIdDesc(id));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkOrderRespVO getWorkOrder(Long id) {
        return workOrderCache.get(id).map(WorkOrderConvert::toRespVO).orElseGet(() -> {
            WorkOrderEntity workOrder = validateWorkOrderExists(id);
            workOrderCache.put(workOrder);
            return WorkOrderConvert.toRespVO(workOrder);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WorkOrderRespVO> getWorkOrderPage(WorkOrderPageReqVO reqVO) {
        Specification<WorkOrderEntity> specification = WorkOrderSpecifications.page(reqVO);
        // 先 count：总数为 0 直接返回空页，省一次列表查询(SQL-005)
        long total = workOrderRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        // 请求页码超过总页数时按最后一页返回(API-009)
        int pageSize = reqVO.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(reqVO.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<WorkOrderEntity> page = workOrderRepository.findAll(specification, pageRequest);
        List<WorkOrderEntity> list = page.getContent();
        return PageResult.of(WorkOrderConvert.toRespVOList(list), total, pageNo, pageSize);
    }

    /**
     * 下达时按 BOM 明细生成工单物料需求：需求数量 = 计划数 × 标准用量 ×(1 + 损耗率/100)，
     * 保留 4 位小数四舍五入(与表字段 decimal(12,4) 对齐)。
     *
     * <p>BOM 必须存在且已生效、明细不为空；重复下达(数据修复等场景)不重复生成。
     *
     * @param workOrder 已下达的工单数据
     */
    private void generateWorkOrderMaterials(WorkOrderEntity workOrder) {
        BomEntity bom = bomRepository.findByIdAndDeletedFalse(workOrder.getBomId())
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.BOM_NOT_AVAILABLE));
        if (!BomStatusEnum.EFFECTIVE.getStatus().equals(bom.getBomStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_NOT_AVAILABLE);
        }
        List<BomDetailEntity> details = bomDetailRepository.findByBomIdAndDeletedFalse(bom.getId());
        if (details.isEmpty()) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_DETAIL_EMPTY);
        }
        if (workOrderMaterialRepository.existsByWorkOrderIdAndDeletedFalse(workOrder.getId())) {
            return;
        }

        BigDecimal planQuantity = BigDecimal.valueOf(workOrder.getPlanQuantity());
        List<WorkOrderMaterialEntity> materials = details.stream().map(detail -> {
            WorkOrderMaterialEntity material = new WorkOrderMaterialEntity();
            material.setWorkOrderId(workOrder.getId());
            material.setMaterialId(detail.getMaterialId());
            material.setRequireQuantity(calculateRequireQuantity(planQuantity, detail));
            return material;
        }).toList();
        workOrderMaterialRepository.saveAll(materials);
    }

    /**
     * 已下达工单计划数量变更后，按当前 BOM 明细重算物料需求。
     *
     * <p>已有需求行原地更新(保留已领数量)，BOM 新增物料补插需求行，
     * BOM 已移除且未领料的需求行逻辑删除(已领料的保留为历史事实)。
     * 新需求数量低于已领数量时说明领料已超出新计划，阻止本次变更并整体回滚。
     *
     * @param workOrderId     工单主键
     * @param bomId           工单 BOM id(已下达工单必已维护)
     * @param newPlanQuantity 变更后的计划数量
     */
    private void recalculateWorkOrderMaterials(Long workOrderId, Long bomId, Integer newPlanQuantity) {
        List<BomDetailEntity> details = bomDetailRepository.findByBomIdAndDeletedFalse(bomId);
        if (details.isEmpty()) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_DETAIL_EMPTY);
        }

        List<WorkOrderMaterialEntity> existingRows =
                workOrderMaterialRepository.findByWorkOrderIdAndDeletedFalse(workOrderId);
        Map<Long, WorkOrderMaterialEntity> rowByMaterialId = existingRows.stream().collect(
                Collectors.toMap(WorkOrderMaterialEntity::getMaterialId, Function.identity(),
                        (first, second) -> first));

        BigDecimal planQuantity = BigDecimal.valueOf(newPlanQuantity);
        List<WorkOrderMaterialEntity> toSave = new ArrayList<>();
        Set<Long> bomMaterialIds = new HashSet<>();
        for (BomDetailEntity detail : details) {
            bomMaterialIds.add(detail.getMaterialId());
            BigDecimal newRequire = calculateRequireQuantity(planQuantity, detail);
            WorkOrderMaterialEntity row = rowByMaterialId.get(detail.getMaterialId());
            if (row == null) {
                row = new WorkOrderMaterialEntity();
                row.setWorkOrderId(workOrderId);
                row.setMaterialId(detail.getMaterialId());
            } else if (newRequire.compareTo(issuedQuantityOf(row)) < 0) {
                throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_MATERIAL_ISSUED_EXCEED);
            }
            row.setRequireQuantity(newRequire);
            toSave.add(row);
        }
        for (WorkOrderMaterialEntity row : existingRows) {
            if (!bomMaterialIds.contains(row.getMaterialId())
                    && issuedQuantityOf(row).compareTo(BigDecimal.ZERO) == 0) {
                row.setDeleted(true);
                toSave.add(row);
            }
        }
        workOrderMaterialRepository.saveAll(toSave);
    }

    /**
     * 计算单个 BOM 明细的物料需求：计划数 × 标准用量 ×(1 + 损耗率/100)，
     * 保留 4 位小数四舍五入(与表字段 decimal(12,4) 对齐)。
     *
     * @param planQuantity 计划数量
     * @param detail       BOM 明细
     * @return 需求数量
     */
    private BigDecimal calculateRequireQuantity(BigDecimal planQuantity, BomDetailEntity detail) {
        BigDecimal lossFactor = BigDecimal.ONE.add(
                detail.getLossRate().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        return planQuantity.multiply(detail.getQuantity()).multiply(lossFactor)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 读取需求行的已领数量，空值按 0 处理。
     *
     * @param row 物料需求行
     * @return 已领数量
     */
    private BigDecimal issuedQuantityOf(WorkOrderMaterialEntity row) {
        return row.getIssuedQuantity() == null ? BigDecimal.ZERO : row.getIssuedQuantity();
    }

    /**
     * 逐个前置状态尝试 CAS 流转：单一前置状态的 UPDATE 命中即可确定真实的
     * 变更前状态，供状态日志与暂停恢复使用，规避"先读后改"窗口内的状态漂移。
     *
     * @param id           工单主键
     * @param fromStatuses 允许的前置状态，按尝试顺序排列
     * @param toStatus     目标状态
     * @return 命中的前置状态；全部未命中返回 null
     */
    private Integer transitionFromAny(Long id, List<Integer> fromStatuses, Integer toStatus) {
        for (Integer fromStatus : fromStatuses) {
            if (workOrderRepository.updateStatus(id, List.of(fromStatus), toStatus) == 1) {
                return fromStatus;
            }
        }
        return null;
    }

    /**
     * 事务提交后再删除详情缓存：提交前删除会被并发读旧值回填，
     * 导致缓存在 TTL 内持续不一致。无事务上下文(单测直连等)时立即删除。
     *
     * @param id 工单主键
     */
    private void evictCacheAfterCommit(Long id) {
        workOrderCache.evictAfterCommit(id);
    }

    /**
     * 校验完工数量不超过 计划数量 ×(1 + 超产比例/100) 向下取整的上限
     * (需求规则：工单执行数量不能超过允许的超产比例)。
     *
     * @param workOrder 工单数据
     */
    private void validateFinishQuantityLimit(WorkOrderEntity workOrder) {
        Integer finishQuantity = workOrder.getFinishQuantity();
        if (finishQuantity == null) {
            return;
        }
        BigDecimal overRatio = workOrder.getOverRatio() == null ? BigDecimal.ZERO : workOrder.getOverRatio();
        int limit = BigDecimal.valueOf(workOrder.getPlanQuantity())
                .multiply(BigDecimal.ONE.add(overRatio.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                .setScale(0, RoundingMode.DOWN)
                .intValue();
        if (finishQuantity > limit) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_FINISH_EXCEED_LIMIT);
        }
    }

    /**
     * 写入工单状态日志。
     *
     * @param workOrderId 工单主键
     * @param fromStatus  变更前状态
     * @param toStatus    变更后状态
     * @param changeType  变更类型
     * @param reason      变更原因，可空
     */
    private void insertStatusLog(Long workOrderId, Integer fromStatus, Integer toStatus,
                                 WorkOrderChangeTypeEnum changeType, String reason) {
        WorkOrderStatusLogEntity statusLog = new WorkOrderStatusLogEntity();
        statusLog.setWorkOrderId(workOrderId);
        statusLog.setFromStatus(fromStatus);
        statusLog.setToStatus(toStatus);
        statusLog.setChangeType(changeType.getType());
        statusLog.setChangeReason(reason);
        statusLog.setOperateBy(SecurityContextHolder.getRequiredLoginUserId());
        statusLog.setOperateTime(LocalDateTime.now());
        workOrderStatusLogRepository.save(statusLog);
    }

    /**
     * 校验计划时间：完成时间不得早于开始时间。
     *
     * @param reqVO 保存请求
     */
    private void validatePlanTime(WorkOrderSaveReqVO reqVO) {
        if (reqVO.getPlanEndTime().isBefore(reqVO.getPlanStartTime())) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_PLAN_TIME_INVALID);
        }
    }

    /**
     * 校验工单存在且未删除。
     *
     * @param id 工单主键
     * @return 工单数据
     */
    private WorkOrderEntity validateWorkOrderExists(Long id) {
        return workOrderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_NOT_EXISTS));
    }

    /**
     * 校验产品存在且处于启用状态。
     *
     * @param productId 产品 id
     * @return 产品档案，用于冗余字段回填
     */
    private ProductEntity validateProduct(Long productId) {
        ProductEntity product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.PRODUCT_NOT_EXISTS));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(product.getStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.PRODUCT_NOT_EXISTS);
        }

        return product;
    }

    /**
     * 校验车间存在且处于启用状态。
     *
     * @param workshopId 车间 id
     */
    private void validateWorkshop(Long workshopId) {
        WorkshopEntity workshop = workshopRepository.findByIdAndDeletedFalse(workshopId)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.WORKSHOP_NOT_EXISTS));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(workshop.getStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.WORKSHOP_NOT_EXISTS);
        }
    }

    /**
     * 确定工单号：外部传入时校验唯一性，未传时由系统生成。
     *
     * @param inputWorkOrderNo 请求中的工单号，可空
     * @return 最终使用的工单号
     */
    private String resolveWorkOrderNo(String inputWorkOrderNo) {
        if (!StringUtils.hasText(inputWorkOrderNo)) {
            return workOrderNoSequence.nextNo();
        }

        // 应用层先查提前给出友好提示；并发窗口由唯一索引兜底(见 insert 的异常转换)
        if (workOrderRepository.existsByWorkOrderNoAndDeletedFalse(inputWorkOrderNo)) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_NO_DUPLICATE);
        }

        return inputWorkOrderNo;
    }
}
