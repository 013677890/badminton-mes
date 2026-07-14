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
 * @author 张竹灏
 * @date 2026/07/13
 */
@Service
public class MaterialStockSyncService {

    private static final Set<Integer> ANALYZABLE_STATUSES = Set.of(
            WorkOrderStatusEnum.RELEASED.getStatus(), WorkOrderStatusEnum.IN_PRODUCTION.getStatus());

    private final MaterialRepository materialRepository;
    private final MaterialStockRepository stockRepository;
    private final WorkOrderMaterialRepository workOrderMaterialRepository;
    private final WorkOrderRepository workOrderRepository;
    private final KitAnalysisService kitAnalysisService;

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
        String sourceSystem = reqVO.getSourceSystem().trim().toUpperCase(Locale.ROOT);
        Set<String> codes = new HashSet<>();
        Set<Long> materialIds = new HashSet<>();
        int acceptedCount = 0;
        for (MaterialStockItemReqVO item : reqVO.getItems()) {
            String materialCode = item.getMaterialCode().trim().toUpperCase(Locale.ROOT);
            if (!codes.add(materialCode)) {
                throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR,
                        "同一请求中物料编码不能重复");
            }
            MaterialEntity material = materialRepository
                    .findByMaterialCodeAndDeletedFalse(materialCode)
                    .orElseThrow(() -> new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR,
                            "物料编码不存在: " + materialCode));
            int affected = stockRepository.upsertIfNewer(
                    material.getId(), sourceSystem, item.getAvailableQuantity(),
                    item.getLockedQuantity(), item.getCheckingQuantity(),
                    item.getTransitQuantity(), item.getSyncTime());
            if (affected > 0) {
                materialIds.add(material.getId());
                acceptedCount++;
            }
        }
        if (Boolean.TRUE.equals(reqVO.getReanalyze()) && !materialIds.isEmpty()) {
            reanalyzeAffectedOrders(materialIds);
        }
        return acceptedCount;
    }

    private void reanalyzeAffectedOrders(Set<Long> materialIds) {
        List<Long> workOrderIds = workOrderMaterialRepository
                .findDistinctWorkOrderIdsByMaterialIdIn(materialIds);
        if (workOrderIds.isEmpty()) {
            return;
        }
        workOrderRepository.findByIdInAndDeletedFalse(workOrderIds).stream()
                .filter(order -> ANALYZABLE_STATUSES.contains(order.getOrderStatus()))
                .forEach(order -> kitAnalysisService.analyzeWorkOrder(order.getId()));
    }
}
