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

/**
 * BOM 明细校验、锁定与持久化组件。
 *
 * <p>该组件集中维护明细数量、损耗率、重复物料和物料可用性规则，并让新增、替换、复制和删除路径共享相同的数据库写入语义。
 */
@Component
public class BomDetailManager {

    private static final int MAX_DETAIL_COUNT = 200;
    private static final BigDecimal MAX_LOSS_RATE = new BigDecimal("100.00");

    private final BomDetailRepository bomDetailRepository;
    private final MaterialRepository materialRepository;

    /** 构造器注入，主表明细和物料主档由同一业务组件协调。 */
    public BomDetailManager(BomDetailRepository bomDetailRepository,
                            MaterialRepository materialRepository) {
        this.bomDetailRepository = bomDetailRepository;
        this.materialRepository = materialRepository;
    }

    /**
     * 校验请求明细，并按物料主键升序加写锁。
     *
     * <p>先在内存中校验数量和重复物料，再统一锁物料行，避免部分明细已锁而后续校验失败；升序锁定用于统一并发锁序。
     */
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

    /** 校验已有明细中的物料仍然可用，并按固定顺序加写锁，供 BOM 生效和工单下达复用。 */
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

    /**
     * 整体替换 BOM 有效明细。
     *
     * <p>旧明细逻辑删除后再插入新明细，两个步骤在调用方事务中完成，失败时整体回滚，不产生半套 BOM。
     */
    public void replace(Long bomId, List<BomDetailSaveReqVO> details) {
        List<BomDetailEntity> oldDetails = getActiveDetails(bomId);
        oldDetails.forEach(detail -> detail.setDeleted(true));
        if (!oldDetails.isEmpty()) {
            bomDetailRepository.saveAllAndFlush(oldDetails);
        }
        insert(bomId, details);
    }

    /** 新增 BOM 有效明细；主表主键由调用方先落库后传入，明细只负责维护外键和业务数量。 */
    public void insert(Long bomId, List<BomDetailSaveReqVO> details) {
        List<BomDetailEntity> entities = details.stream()
                .map(detail -> newDetail(bomId, detail.getMaterialId(),
                        detail.getQuantity(), detail.getLossRate()))
                .toList();
        bomDetailRepository.saveAllAndFlush(entities);
    }

    /** 将来源明细复制到新 BOM，只复制当前有效行，不复制来源行的历史主键和删除标记。 */
    public void copyTo(Long targetBomId, List<BomDetailEntity> sourceDetails) {
        List<BomDetailEntity> copies = sourceDetails.stream()
                .map(detail -> newDetail(targetBomId, detail.getMaterialId(),
                        detail.getQuantity(), detail.getLossRate()))
                .toList();
        bomDetailRepository.saveAllAndFlush(copies);
    }

    /** 逻辑删除 BOM 全部有效明细，保留历史版本的数量与损耗率快照。 */
    public void softDelete(Long bomId) {
        List<BomDetailEntity> details = getActiveDetails(bomId);
        details.forEach(detail -> detail.setDeleted(true));
        if (!details.isEmpty()) {
            bomDetailRepository.saveAllAndFlush(details);
        }
    }

    /** 查询有效明细并按主键稳定排序，使响应、复制和重算结果具有确定顺序。 */
    public List<BomDetailEntity> getActiveDetails(Long bomId) {
        List<BomDetailEntity> details = new ArrayList<>(
                bomDetailRepository.findByBomIdAndDeletedFalse(bomId));
        details.sort(Comparator.comparing(BomDetailEntity::getId));
        return details;
    }

    /**
     * 批量查询明细物料，包含已停用或已逻辑删除的历史档案。
     *
     * <p>详情展示需要回填历史名称，因此这里不能复用“仅启用物料”的校验查询。
     */
    public Map<Long, MaterialEntity> getMaterialMap(List<BomDetailEntity> details) {
        Set<Long> materialIds = details.stream().map(BomDetailEntity::getMaterialId)
                .collect(Collectors.toSet());
        if (materialIds.isEmpty()) {
            return Map.of();
        }
        return materialRepository.findAllById(materialIds).stream()
                .collect(Collectors.toMap(MaterialEntity::getId, Function.identity()));
    }

    /**
     * 按固定顺序锁定并校验物料。
     *
     * <p>查询结果数量必须与请求物料数一致，且每行都处于启用状态；任一物料缺失或停用都会使整个写事务失败。
     */
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

    /** 校验单条明细数值，确保数量为正、损耗率在 0% 到 100% 的业务范围内。 */
    private boolean isValid(BomDetailSaveReqVO detail) {
        return detail != null && detail.getMaterialId() != null && detail.getMaterialId() > 0
                && detail.getQuantity() != null && detail.getQuantity().signum() > 0
                && detail.getLossRate() != null && detail.getLossRate().signum() >= 0
                && detail.getLossRate().compareTo(MAX_LOSS_RATE) <= 0;
    }

    /** 构造新明细实体，不复制数据库审计字段和旧明细主键。 */
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
