package com.badminton.mes.module.production.service.support;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.BomDetailSaveReqVO;
import com.badminton.mes.module.production.dal.entity.BomDetailEntity;
import com.badminton.mes.module.production.dal.entity.MaterialEntity;
import com.badminton.mes.module.production.dal.repository.BomDetailRepository;
import com.badminton.mes.module.production.dal.repository.MaterialRepository;

import org.springframework.stereotype.Component;

/** BOM 明细校验、锁定与持久化组件。 */
@Component
public class BomDetailManager {

    private static final int MAX_DETAIL_COUNT = 200;
    private static final BigDecimal MAX_LOSS_RATE = new BigDecimal("100.00");

    private final BomDetailRepository bomDetailRepository;
    private final MaterialRepository materialRepository;

    /** 构造器注入。 */
    public BomDetailManager(BomDetailRepository bomDetailRepository,
                            MaterialRepository materialRepository) {
        this.bomDetailRepository = bomDetailRepository;
        this.materialRepository = materialRepository;
    }

    /** 校验请求明细，并按物料主键升序加写锁。 */
    public void validateAndLock(List<BomDetailSaveReqVO> details) {
        if (details == null || details.isEmpty() || details.size() > MAX_DETAIL_COUNT) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_DETAIL_INVALID);
        }
        Set<Long> materialIds = new LinkedHashSet<>();
        for (BomDetailSaveReqVO detail : details) {
            if (!isValid(detail) || !materialIds.add(detail.getMaterialId())) {
                throw new ServiceException(ProductionErrorCodeConstants.BOM_DETAIL_INVALID);
            }
        }
        lockEnabledMaterials(materialIds);
    }

    /** 校验已有明细中的物料仍然可用，并加写锁。 */
    public void validateAndLockExisting(List<BomDetailEntity> details) {
        if (details == null || details.isEmpty() || details.size() > MAX_DETAIL_COUNT) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_DETAIL_INVALID);
        }
        Set<Long> materialIds = details.stream().map(BomDetailEntity::getMaterialId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (materialIds.size() != details.size()) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_DETAIL_INVALID);
        }
        lockEnabledMaterials(materialIds);
    }

    /** 整体替换 BOM 有效明细。 */
    public void replace(Long bomId, List<BomDetailSaveReqVO> details) {
        List<BomDetailEntity> oldDetails = getActiveDetails(bomId);
        oldDetails.forEach(detail -> detail.setDeleted(true));
        if (!oldDetails.isEmpty()) {
            bomDetailRepository.saveAllAndFlush(oldDetails);
        }
        insert(bomId, details);
    }

    /** 新增 BOM 有效明细。 */
    public void insert(Long bomId, List<BomDetailSaveReqVO> details) {
        List<BomDetailEntity> entities = details.stream()
                .map(detail -> newDetail(bomId, detail.getMaterialId(),
                        detail.getQuantity(), detail.getLossRate()))
                .toList();
        bomDetailRepository.saveAllAndFlush(entities);
    }

    /** 将来源明细复制到新 BOM。 */
    public void copyTo(Long targetBomId, List<BomDetailEntity> sourceDetails) {
        List<BomDetailEntity> copies = sourceDetails.stream()
                .map(detail -> newDetail(targetBomId, detail.getMaterialId(),
                        detail.getQuantity(), detail.getLossRate()))
                .toList();
        bomDetailRepository.saveAllAndFlush(copies);
    }

    /** 逻辑删除 BOM 全部有效明细。 */
    public void softDelete(Long bomId) {
        List<BomDetailEntity> details = getActiveDetails(bomId);
        details.forEach(detail -> detail.setDeleted(true));
        if (!details.isEmpty()) {
            bomDetailRepository.saveAllAndFlush(details);
        }
    }

    /** 查询并按主键稳定排序有效明细。 */
    public List<BomDetailEntity> getActiveDetails(Long bomId) {
        List<BomDetailEntity> details = new ArrayList<>(
                bomDetailRepository.findByBomIdAndDeletedFalse(bomId));
        details.sort(Comparator.comparing(BomDetailEntity::getId));
        return details;
    }

    /** 批量查询明细物料，包含已停用或已逻辑删除的历史档案。 */
    public Map<Long, MaterialEntity> getMaterialMap(List<BomDetailEntity> details) {
        Set<Long> materialIds = details.stream().map(BomDetailEntity::getMaterialId)
                .collect(Collectors.toSet());
        if (materialIds.isEmpty()) {
            return Map.of();
        }
        return materialRepository.findAllById(materialIds).stream()
                .collect(Collectors.toMap(MaterialEntity::getId, Function.identity()));
    }

    /** 按固定顺序锁定并校验物料。 */
    private void lockEnabledMaterials(Collection<Long> materialIds) {
        List<Long> sortedIds = materialIds.stream().sorted().toList();
        List<MaterialEntity> materials = materialRepository
                .findAllByIdInForUpdateOrderByIdAsc(sortedIds);
        boolean unavailable = materials.size() != sortedIds.size()
                || materials.stream().anyMatch(material -> !CommonStatusEnum.ENABLED.getStatus()
                        .equals(material.getStatus()));
        if (unavailable) {
            throw new ServiceException(ProductionErrorCodeConstants.BOM_MATERIAL_NOT_AVAILABLE);
        }
    }

    /** 校验单条明细数值。 */
    private boolean isValid(BomDetailSaveReqVO detail) {
        return detail != null && detail.getMaterialId() != null && detail.getMaterialId() > 0
                && detail.getQuantity() != null && detail.getQuantity().signum() > 0
                && detail.getLossRate() != null && detail.getLossRate().signum() >= 0
                && detail.getLossRate().compareTo(MAX_LOSS_RATE) <= 0;
    }

    /** 构造新明细实体。 */
    private BomDetailEntity newDetail(Long bomId, Long materialId,
                                      BigDecimal quantity, BigDecimal lossRate) {
        BomDetailEntity detail = new BomDetailEntity();
        detail.setBomId(bomId);
        detail.setMaterialId(materialId);
        detail.setQuantity(quantity);
        detail.setLossRate(lossRate);
        return detail;
    }
}
