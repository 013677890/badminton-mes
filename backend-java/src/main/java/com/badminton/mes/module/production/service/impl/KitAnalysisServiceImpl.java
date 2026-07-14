package com.badminton.mes.module.production.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.KitAnalysisRespVO;
import com.badminton.mes.module.production.controller.vo.ShortageBoardRespVO;
import com.badminton.mes.module.production.controller.vo.ShortageHandleRespVO;
import com.badminton.mes.module.production.controller.vo.ShortageHandleSaveReqVO;
import com.badminton.mes.module.production.controller.vo.ShortageOrderRespVO;
import com.badminton.mes.module.production.dal.entity.KitAnalysisEntity;
import com.badminton.mes.module.production.dal.entity.KitShortageHandleEntity;
import com.badminton.mes.module.production.dal.entity.MaterialEntity;
import com.badminton.mes.module.production.dal.entity.MaterialStockEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderMaterialEntity;
import com.badminton.mes.module.production.dal.redis.WorkOrderCache;
import com.badminton.mes.module.production.dal.repository.KitAnalysisRepository;
import com.badminton.mes.module.production.dal.repository.KitShortageHandleRepository;
import com.badminton.mes.module.production.dal.repository.MaterialRepository;
import com.badminton.mes.module.production.dal.repository.MaterialStockRepository;
import com.badminton.mes.module.production.dal.repository.ShortageBoardProjection;
import com.badminton.mes.module.production.dal.repository.WorkOrderMaterialRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.enums.KitStatusEnum;
import com.badminton.mes.module.production.enums.ShortageHandleStatusEnum;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;
import com.badminton.mes.module.production.service.KitAnalysisService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 齐套分析 Service 实现。
 *
 * <p>计算规则对应业务 SQL 1.3，改为应用层实现(明细规模小、便于单测)：
 * 可用 = max(可用-锁定-在检, 0)，欠料 = max(剩余需求-可用, 0)，
 * 工单级状态取各物料行最大值(欠料>0 不得标记齐套)。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Service
public class KitAnalysisServiceImpl implements KitAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(KitAnalysisServiceImpl.class);

    /** 允许齐套分析的工单状态：已下达、生产中 */
    private static final Set<Integer> ANALYZABLE_STATUSES = Set.of(
            WorkOrderStatusEnum.RELEASED.getStatus(), WorkOrderStatusEnum.IN_PRODUCTION.getStatus());

    private final WorkOrderRepository workOrderRepository;

    private final WorkOrderMaterialRepository workOrderMaterialRepository;

    private final MaterialStockRepository materialStockRepository;

    private final KitAnalysisRepository kitAnalysisRepository;

    private final KitShortageHandleRepository kitShortageHandleRepository;

    private final MaterialRepository materialRepository;

    private final WorkOrderCache workOrderCache;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param workOrderRepository         工单 Repository
     * @param workOrderMaterialRepository 工单物料需求 Repository
     * @param materialStockRepository     库存快照 Repository
     * @param kitAnalysisRepository       齐套结果 Repository
     * @param kitShortageHandleRepository 欠料处理记录 Repository
     * @param materialRepository          物料 Repository
     * @param workOrderCache              工单详情缓存
     */
    public KitAnalysisServiceImpl(WorkOrderRepository workOrderRepository,
                                  WorkOrderMaterialRepository workOrderMaterialRepository,
                                  MaterialStockRepository materialStockRepository,
                                  KitAnalysisRepository kitAnalysisRepository,
                                  KitShortageHandleRepository kitShortageHandleRepository,
                                  MaterialRepository materialRepository,
                                  WorkOrderCache workOrderCache) {
        this.workOrderRepository = workOrderRepository;
        this.workOrderMaterialRepository = workOrderMaterialRepository;
        this.materialStockRepository = materialStockRepository;
        this.kitAnalysisRepository = kitAnalysisRepository;
        this.kitShortageHandleRepository = kitShortageHandleRepository;
        this.materialRepository = materialRepository;
        this.workOrderCache = workOrderCache;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer analyzeWorkOrder(Long workOrderId) {
        // 悲观锁锁工单行：串行化"软删旧结果+插新结果"，防并发重算残留双份
        WorkOrderEntity workOrder = workOrderRepository.findByIdForUpdate(workOrderId)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_NOT_EXISTS));
        if (!ANALYZABLE_STATUSES.contains(workOrder.getOrderStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.KIT_ANALYSIS_ORDER_STATUS_INVALID);
        }
        List<WorkOrderMaterialEntity> materials =
                workOrderMaterialRepository.findByWorkOrderIdAndDeletedFalse(workOrderId);
        if (materials.isEmpty()) {
            throw new ServiceException(ProductionErrorCodeConstants.KIT_ANALYSIS_MATERIAL_EMPTY);
        }

        List<Long> materialIds = materials.stream().map(WorkOrderMaterialEntity::getMaterialId).toList();
        Map<Long, MaterialStockEntity> stockMap =
                materialStockRepository.findByMaterialIdInAndDeletedFalse(materialIds).stream()
                        .collect(Collectors.toMap(MaterialStockEntity::getMaterialId, Function.identity(),
                                (first, second) -> first));

        LocalDateTime analysisTime = LocalDateTime.now();
        List<KitAnalysisEntity> results = new ArrayList<>(materials.size());
        int orderKitStatus = KitStatusEnum.COMPLETE.getStatus();
        for (WorkOrderMaterialEntity material : materials) {
            KitAnalysisEntity result = calculateLine(material, stockMap.get(material.getMaterialId()), analysisTime);
            orderKitStatus = Math.max(orderKitStatus, result.getKitStatus());
            results.add(result);
        }

        kitAnalysisRepository.softDeleteByWorkOrderId(workOrderId);
        kitAnalysisRepository.saveAll(results);
        workOrderRepository.updateKitStatus(workOrderId, orderKitStatus);
        // kit_status 属缓存实体字段，提交后删缓存，避免并发读旧值回填 Redis
        workOrderCache.evictAfterCommit(workOrderId);
        logger.info("[齐套分析完成] workOrderId: {}, kitStatus: {}", workOrderId, orderKitStatus);
        return orderKitStatus;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KitAnalysisRespVO> getKitResult(Long workOrderId) {
        List<KitAnalysisEntity> results = kitAnalysisRepository.findByWorkOrderIdAndDeletedFalse(workOrderId);
        if (results.isEmpty()) {
            return List.of();
        }
        Map<Long, MaterialEntity> materialMap = loadMaterialMap(
                results.stream().map(KitAnalysisEntity::getMaterialId).toList());
        return results.stream().map(entity -> {
            KitAnalysisRespVO respVO = new KitAnalysisRespVO();
            respVO.setId(entity.getId());
            respVO.setWorkOrderId(entity.getWorkOrderId());
            respVO.setMaterialId(entity.getMaterialId());
            fillMaterialInfo(materialMap.get(entity.getMaterialId()),
                    respVO::setMaterialCode, respVO::setMaterialName);
            respVO.setRequireQuantity(entity.getRequireQuantity());
            respVO.setAvailableQuantity(entity.getAvailableQuantity());
            respVO.setTransitQuantity(entity.getTransitQuantity());
            respVO.setShortageQuantity(entity.getShortageQuantity());
            respVO.setKitStatus(entity.getKitStatus());
            respVO.setAnalysisTime(entity.getAnalysisTime());
            return respVO;
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShortageBoardRespVO> getShortageBoard() {
        List<ShortageBoardProjection> aggregates = kitAnalysisRepository.aggregateShortageBoard();
        if (aggregates.isEmpty()) {
            return List.of();
        }
        Map<Long, MaterialEntity> materialMap = loadMaterialMap(
                aggregates.stream().map(ShortageBoardProjection::getMaterialId).toList());
        return aggregates.stream().map(aggregate -> {
            ShortageBoardRespVO respVO = new ShortageBoardRespVO();
            respVO.setMaterialId(aggregate.getMaterialId());
            fillMaterialInfo(materialMap.get(aggregate.getMaterialId()),
                    respVO::setMaterialCode, respVO::setMaterialName);
            respVO.setTotalShortage(aggregate.getTotalShortage());
            respVO.setAffectedOrderCount(aggregate.getAffectedOrderCount());
            respVO.setTransitQuantity(aggregate.getTransitQuantity());
            kitShortageHandleRepository
                    .findFirstByMaterialIdAndHandleStatusAndDeletedFalseOrderByIdDesc(
                            aggregate.getMaterialId(), ShortageHandleStatusEnum.PROCESSING.getStatus())
                    .ifPresent(handle -> respVO.setExpectedArrivalDate(handle.getExpectedArrivalDate()));
            return respVO;
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShortageOrderRespVO> getShortageOrdersByMaterial(Long materialId) {
        List<KitAnalysisEntity> lines = kitAnalysisRepository
                .findByMaterialIdAndShortageQuantityGreaterThanAndDeletedFalse(materialId, BigDecimal.ZERO);
        if (lines.isEmpty()) {
            return List.of();
        }
        Map<Long, WorkOrderEntity> workOrderMap = workOrderRepository
                .findByIdInAndDeletedFalse(lines.stream().map(KitAnalysisEntity::getWorkOrderId).toList())
                .stream()
                .collect(Collectors.toMap(WorkOrderEntity::getId, Function.identity(), (first, second) -> first));
        return lines.stream().map(line -> {
            ShortageOrderRespVO respVO = new ShortageOrderRespVO();
            respVO.setWorkOrderId(line.getWorkOrderId());
            WorkOrderEntity workOrder = workOrderMap.get(line.getWorkOrderId());
            if (workOrder != null) {
                respVO.setWorkOrderNo(workOrder.getWorkOrderNo());
                respVO.setProductName(workOrder.getProductName());
            }
            respVO.setRequireQuantity(line.getRequireQuantity());
            respVO.setAvailableQuantity(line.getAvailableQuantity());
            respVO.setShortageQuantity(line.getShortageQuantity());
            return respVO;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createShortageHandle(ShortageHandleSaveReqVO reqVO) {
        workOrderRepository.findByIdAndDeletedFalse(reqVO.getWorkOrderId())
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_NOT_EXISTS));
        KitShortageHandleEntity entity = new KitShortageHandleEntity();
        entity.setWorkOrderId(reqVO.getWorkOrderId());
        entity.setMaterialId(reqVO.getMaterialId());
        entity.setHandleType(reqVO.getHandleType());
        entity.setHandlerId(reqVO.getHandlerId());
        entity.setExpectedArrivalDate(reqVO.getExpectedArrivalDate());
        entity.setHandleRemark(reqVO.getHandleRemark());
        entity.setHandleStatus(ShortageHandleStatusEnum.PROCESSING.getStatus());
        entity.setCreateBy(SecurityContextHolder.getRequiredLoginUserId());
        kitShortageHandleRepository.save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveShortageHandle(Long id) {
        int rows = kitShortageHandleRepository.updateHandleStatus(id,
                ShortageHandleStatusEnum.PROCESSING.getStatus(), ShortageHandleStatusEnum.RESOLVED.getStatus());
        if (rows == 1) {
            return;
        }

        // CAS 未命中查因：不存在给 A0402，已解决给 A0506(防并发重复解决)
        kitShortageHandleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.SHORTAGE_HANDLE_NOT_EXISTS));
        throw new ServiceException(ProductionErrorCodeConstants.SHORTAGE_HANDLE_ALREADY_RESOLVED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShortageHandleRespVO> getShortageHandles(Long workOrderId) {
        List<KitShortageHandleEntity> handles =
                kitShortageHandleRepository.findByWorkOrderIdAndDeletedFalseOrderByIdDesc(workOrderId);
        if (handles.isEmpty()) {
            return List.of();
        }
        Map<Long, MaterialEntity> materialMap = loadMaterialMap(
                handles.stream().map(KitShortageHandleEntity::getMaterialId).toList());
        return handles.stream().map(entity -> {
            ShortageHandleRespVO respVO = new ShortageHandleRespVO();
            respVO.setId(entity.getId());
            respVO.setWorkOrderId(entity.getWorkOrderId());
            respVO.setMaterialId(entity.getMaterialId());
            fillMaterialInfo(materialMap.get(entity.getMaterialId()),
                    respVO::setMaterialCode, respVO::setMaterialName);
            respVO.setHandleType(entity.getHandleType());
            respVO.setHandlerId(entity.getHandlerId());
            respVO.setExpectedArrivalDate(entity.getExpectedArrivalDate());
            respVO.setHandleRemark(entity.getHandleRemark());
            respVO.setHandleStatus(entity.getHandleStatus());
            respVO.setCreateTime(entity.getCreateTime());
            return respVO;
        }).toList();
    }

    /**
     * 单物料行齐套计算：可用扣锁定与在检，欠料不为负；
     * 行状态：剩余需求≤可用→齐套，可用>0→部分齐套，否则欠料。
     *
     * @param material     工单物料需求行
     * @param stock        库存快照，可能为 null(按可用 0 处理)
     * @param analysisTime 本次分析时间
     * @return 齐套结果行
     */
    private KitAnalysisEntity calculateLine(WorkOrderMaterialEntity material,
                                            MaterialStockEntity stock, LocalDateTime analysisTime) {
        BigDecimal issued = material.getIssuedQuantity() == null ? BigDecimal.ZERO : material.getIssuedQuantity();
        BigDecimal remain = material.getRequireQuantity().subtract(issued);
        BigDecimal available = BigDecimal.ZERO;
        BigDecimal transit = BigDecimal.ZERO;
        if (stock != null) {
            BigDecimal rawAvailable = stock.getAvailableQuantity()
                    .subtract(stock.getLockedQuantity())
                    .subtract(stock.getCheckingQuantity());
            if (rawAvailable.signum() < 0) {
                logger.warn("[齐套分析净可用为负] materialId: {}, rawAvailable: {}，按 0 参与计算，建议核查库存锁定/在检数据",
                        material.getMaterialId(), rawAvailable);
            }
            available = rawAvailable.max(BigDecimal.ZERO);
            transit = stock.getTransitQuantity();
        }
        BigDecimal shortage = remain.subtract(available).max(BigDecimal.ZERO);

        KitAnalysisEntity result = new KitAnalysisEntity();
        result.setWorkOrderId(material.getWorkOrderId());
        result.setMaterialId(material.getMaterialId());
        result.setRequireQuantity(remain);
        result.setAvailableQuantity(available);
        result.setTransitQuantity(transit);
        result.setShortageQuantity(shortage);
        if (remain.compareTo(available) <= 0) {
            result.setKitStatus(KitStatusEnum.COMPLETE.getStatus());
        } else if (available.compareTo(BigDecimal.ZERO) > 0) {
            result.setKitStatus(KitStatusEnum.PARTIAL.getStatus());
        } else {
            result.setKitStatus(KitStatusEnum.SHORTAGE.getStatus());
        }
        result.setAnalysisTime(analysisTime);
        return result;
    }

    /**
     * 批量回查物料档案，用于编码/名称回填，避免逐行 N+1。
     *
     * @param materialIds 物料主键集合
     * @return 物料 id -> 物料实体
     */
    private Map<Long, MaterialEntity> loadMaterialMap(List<Long> materialIds) {
        return materialRepository.findByIdInAndDeletedFalse(materialIds.stream().distinct().toList())
                .stream()
                .collect(Collectors.toMap(MaterialEntity::getId, Function.identity(), (first, second) -> first));
    }

    /**
     * 回填物料编码/名称，物料档案缺失时保持空。
     *
     * @param material     物料实体，可能为 null
     * @param codeConsumer 编码 setter
     * @param nameConsumer 名称 setter
     */
    private void fillMaterialInfo(MaterialEntity material,
                                  Consumer<String> codeConsumer,
                                  Consumer<String> nameConsumer) {
        if (material != null) {
            codeConsumer.accept(material.getMaterialCode());
            nameConsumer.accept(material.getMaterialName());
        }
    }
}
