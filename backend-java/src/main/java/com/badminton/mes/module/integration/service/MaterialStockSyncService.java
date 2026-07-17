package com.badminton.mes.module.integration.service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.integration.controller.vo.MaterialStockBatchReqVO;
import com.badminton.mes.module.integration.controller.vo.MaterialStockItemReqVO;
import com.badminton.mes.module.production.dal.entity.MaterialEntity;
import com.badminton.mes.module.production.dal.repository.MaterialRepository;
import com.badminton.mes.module.production.dal.repository.MaterialStockRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderMaterialRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;
import com.badminton.mes.module.production.service.KitAnalysisService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * WMS/ERP 库存与在途快照同步服务。
 *
 * <p>每个来源系统按物料维护最新库存快照，数据库 upsert 只接受同步时间更新的数据，防止网络
 * 乱序让旧快照覆盖新库存。批次中真正被数据库接受的物料才会触发受影响工单的齐套重算，
 * 并且只重算已下达或生产中的工单，避免无效计算和历史状态被意外改写。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Service
public class MaterialStockSyncService {

    /** 允许因库存变化重新计算齐套性的工单状态集合。 */
    private static final Set<Integer> ANALYZABLE_STATUSES = Set.of(
            WorkOrderStatusEnum.RELEASED.getStatus(), WorkOrderStatusEnum.IN_PRODUCTION.getStatus());

    /** 物料主档仓储，用于将外部物料编码解析为 MES 主键。 */
    private final MaterialRepository materialRepository;

    /** 库存快照仓储，使用带同步时间条件的数据库 upsert 防止旧数据覆盖。 */
    private final MaterialStockRepository stockRepository;

    /** 工单物料关系仓储，用于反查库存变化影响到的生产工单。 */
    private final WorkOrderMaterialRepository workOrderMaterialRepository;

    /** 生产工单仓储，用于批量读取并筛选当前可重新分析的工单。 */
    private final WorkOrderRepository workOrderRepository;

    /** 齐套分析服务，按最新库存重新计算受影响工单的物料满足情况。 */
    private final KitAnalysisService kitAnalysisService;

    /** 构造库存同步服务并固定主档、库存、工单关系及齐套分析依赖。 */
    public MaterialStockSyncService(MaterialRepository materialRepository,
                                    MaterialStockRepository stockRepository,
                                    WorkOrderMaterialRepository workOrderMaterialRepository,
                                    WorkOrderRepository workOrderRepository,
                                    KitAnalysisService kitAnalysisService) {
        this.materialRepository = materialRepository;
        this.stockRepository = stockRepository;
        this.workOrderMaterialRepository = workOrderMaterialRepository;
        this.workOrderRepository = workOrderRepository;
        this.kitAnalysisService = kitAnalysisService;
    }

    /** 批量更新库存快照，并按需重新执行受影响工单齐套分析。 */
    @Transactional(rollbackFor = Exception.class)
    public int sync(MaterialStockBatchReqVO reqVO) {
        // 来源系统统一去空格并大写，保证同一上游不会因大小写差异生成多套库存快照。
        String sourceSystem = reqVO.getSourceSystem().trim().toUpperCase(Locale.ROOT);
        // codes 用于批内防重，materialIds 只收集实际接受新快照的物料主键。
        Set<String> codes = new HashSet<>();
        Set<Long> materialIds = new HashSet<>();
        int acceptedCount = 0;
        for (MaterialStockItemReqVO item : reqVO.getItems()) {
            String materialCode = item.getMaterialCode().trim().toUpperCase(Locale.ROOT);
            if (!codes.add(materialCode)) {
                // 同批重复物料的先后覆盖语义不明确，整批回滚并要求调用方消除歧义。
                throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR,
                        "同一请求中物料编码不能重复");
            }
            // 外部库存必须绑定现有有效物料，接口不隐式创建主档。
            MaterialEntity material = materialRepository
                    .findByMaterialCodeAndDeletedFalse(materialCode)
                    .orElseThrow(() -> new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR,
                            "物料编码不存在: " + materialCode));
            // 数据库仅在不存在快照或本次 syncTime 更新时插入/更新，天然抵抗乱序消息。
            int affected = stockRepository.upsertIfNewer(
                    material.getId(), sourceSystem, item.getAvailableQuantity(),
                    item.getLockedQuantity(), item.getCheckingQuantity(),
                    item.getTransitQuantity(), item.getSyncTime());
            if (affected > 0) {
                // 只有数据库真正采纳的新快照才计入返回值和后续齐套重算范围。
                materialIds.add(material.getId());
                acceptedCount++;
            }
        }
        if (Boolean.TRUE.equals(reqVO.getReanalyze()) && !materialIds.isEmpty()) {
            // 重算由调用方显式选择，且没有快照变化时不执行无意义的工单查询。
            reanalyzeAffectedOrders(materialIds);
        }
        return acceptedCount;
    }

    /** 根据发生变化的物料反查工单，并仅重算当前仍具生产意义的状态。 */
    private void reanalyzeAffectedOrders(Set<Long> materialIds) {
        // 关系表使用 DISTINCT 避免同一工单因包含多个变化物料而被重复分析。
        List<Long> workOrderIds = workOrderMaterialRepository
                .findDistinctWorkOrderIdsByMaterialIdIn(materialIds);
        if (workOrderIds.isEmpty()) {
            return;
        }
        // 批量加载工单后在内存筛选状态，避免逐个主键查询造成 N+1 往返。
        workOrderRepository.findByIdInAndDeletedFalse(workOrderIds).stream()
                .filter(order -> ANALYZABLE_STATUSES.contains(order.getOrderStatus()))
                .forEach(order -> kitAnalysisService.analyzeWorkOrder(order.getId()));
    }
}
