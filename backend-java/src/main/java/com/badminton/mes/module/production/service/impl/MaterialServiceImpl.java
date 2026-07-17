package com.badminton.mes.module.production.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.integration.service.UnitService;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.MaterialPageReqVO;
import com.badminton.mes.module.production.controller.vo.MaterialRespVO;
import com.badminton.mes.module.production.controller.vo.MaterialSaveReqVO;
import com.badminton.mes.module.production.controller.vo.MaterialUpdateReqVO;
import com.badminton.mes.module.production.controller.vo.ProductionStatusReqVO;
import com.badminton.mes.module.production.convert.ProductionMasterDataConvert;
import com.badminton.mes.module.production.dal.entity.MaterialEntity;
import com.badminton.mes.module.production.dal.repository.BomDetailRepository;
import com.badminton.mes.module.production.dal.repository.KitAnalysisRepository;
import com.badminton.mes.module.production.dal.repository.MaterialRepository;
import com.badminton.mes.module.production.dal.repository.MaterialStockRepository;
import com.badminton.mes.module.production.dal.repository.ProductionMasterDataSpecifications;
import com.badminton.mes.module.production.dal.repository.WorkOrderMaterialRepository;
import com.badminton.mes.module.production.enums.BomStatusEnum;
import com.badminton.mes.module.production.enums.MaterialTypeEnum;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;
import com.badminton.mes.module.production.service.MaterialService;
import com.badminton.mes.module.production.service.support.ProductionPersistenceExceptionTranslator;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 物料主档服务实现。 */
@Service
public class MaterialServiceImpl implements MaterialService {

    private static final String MATERIAL_CODE_CONSTRAINT = "uk_active_material_code";
    private final MaterialRepository materialRepository;
    private final UnitService unitService;
    private final BomDetailRepository bomDetailRepository;
    private final WorkOrderMaterialRepository workOrderMaterialRepository;
    private final MaterialStockRepository materialStockRepository;
    private final KitAnalysisRepository kitAnalysisRepository;

    /** 构造器注入。 */
    public MaterialServiceImpl(MaterialRepository materialRepository,
                               UnitService unitService,
                               BomDetailRepository bomDetailRepository,
                               WorkOrderMaterialRepository workOrderMaterialRepository,
                               MaterialStockRepository materialStockRepository,
                               KitAnalysisRepository kitAnalysisRepository) {
        this.materialRepository = materialRepository;
        this.unitService = unitService;
        this.bomDetailRepository = bomDetailRepository;
        this.workOrderMaterialRepository = workOrderMaterialRepository;
        this.materialStockRepository = materialStockRepository;
        this.kitAnalysisRepository = kitAnalysisRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMaterial(MaterialSaveReqVO reqVO) {
        // 先规范化编码、名称和规格，再执行类型、状态、单位及编码校验，保证查重值与实际入库值一致。
        normalize(reqVO);
        validateType(reqVO.getMaterialType());
        validateStatus(reqVO.getStatus());
        validateCode(reqVO.getMaterialCode(), null);
        requireEnabledUnitForUpdate(reqVO.getUnitId());
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        MaterialEntity material = ProductionMasterDataConvert.toMaterialEntity(reqVO);
        material.setCreateBy(operatorId);
        material.setUpdateBy(operatorId);
        // saveAndFlush 会在当前事务内尽早触发唯一索引检查，save 方法负责将数据库异常转换为业务错误码。
        save(material);
        return material.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMaterial(Long id, MaterialUpdateReqVO reqVO) {
        // 先锁定主档行并校验版本，后续引用检查和字段更新均基于同一份数据库快照。
        normalize(reqVO);
        validateType(reqVO.getMaterialType());
        validateStatus(reqVO.getStatus());
        MaterialEntity material = requireForUpdate(id);
        validateVersion(material, reqVO.getVersion());
        validateCode(reqVO.getMaterialCode(), id);
        requireEnabledUnitForUpdate(reqVO.getUnitId());
        if (!Objects.equals(material.getUnitId(), reqVO.getUnitId()) && hasAnyReferences(id)) {
            // 已被 BOM、工单、库存或齐套分析引用的物料，单位变化会破坏历史数量语义，因此禁止修改。
            throw new ServiceException(ProductionErrorCodeConstants.MATERIAL_UNIT_IMMUTABLE);
        }
        if (CommonStatusEnum.DISABLED.getStatus().equals(reqVO.getStatus())
                && !CommonStatusEnum.DISABLED.getStatus().equals(material.getStatus())) {
            // 停用前检查生效 BOM 和活动工单，避免新业务继续引用已停用物料。
            validateNoActiveReferences(id);
        }
        ProductionMasterDataConvert.copyMaterial(reqVO, material);
        material.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        save(material);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMaterial(Long id, Integer version) {
        // 物料采用逻辑删除；保留历史引用所需的原始行，避免外键语义和追溯数据断裂。
        MaterialEntity material = requireForUpdate(id);
        validateVersion(material, version);
        validateNoAnyReferences(id);
        material.setDeleted(true);
        material.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        save(material);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMaterialStatus(Long id, ProductionStatusReqVO reqVO) {
        // 状态变更单独使用锁版本控制；重复提交相同状态直接幂等返回，不重复写审计字段。
        validateStatus(reqVO.getStatus());
        MaterialEntity material = requireForUpdate(id);
        validateVersion(material, reqVO.getVersion());
        if (Objects.equals(material.getStatus(), reqVO.getStatus())) {
            return;
        }
        if (CommonStatusEnum.ENABLED.getStatus().equals(reqVO.getStatus())) {
            // 重新启用前要求计量单位仍然存在且启用，防止得到不可用于业务单据的物料主档。
            requireEnabledUnitForUpdate(material.getUnitId());
        } else {
            // 停用只阻断后续有效引用，不删除已存在的历史需求、库存和分析记录。
            validateNoActiveReferences(id);
        }
        material.setStatus(reqVO.getStatus());
        material.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        save(material);
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialRespVO getMaterial(Long id) {
        // 详情只读取未删除主档并转换为响应对象，避免直接暴露 JPA 实体及内部审计字段。
        return ProductionMasterDataConvert.toMaterialRespVO(require(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MaterialRespVO> getMaterialPage(MaterialPageReqVO reqVO) {
        // 先校验筛选枚举，再 count；无数据时不再执行列表 SQL，并将超出范围页码收敛到最后一页。
        validateOptionalType(reqVO.getMaterialType());
        var specification = ProductionMasterDataSpecifications.materialPage(reqVO);
        long total = materialRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }
        int pageNo = normalizePageNo(reqVO.getPageNo(), reqVO.getPageSize(), total);
        Page<MaterialEntity> page = materialRepository.findAll(specification,
                PageRequest.of(pageNo - 1, reqVO.getPageSize(),
                        Sort.by(Sort.Direction.ASC, "materialCode").and(Sort.by(Sort.Direction.DESC, "id"))));
        List<MaterialRespVO> list = page.getContent().stream()
                .map(ProductionMasterDataConvert::toMaterialRespVO).toList();
        return PageResult.of(list, total, pageNo, reqVO.getPageSize());
    }

    /**
     * 校验不存在生效 BOM 或未结束工单引用。
     *
     * <p>两个 exists 查询只返回布尔值，不加载明细实体，适合状态变更前的轻量数据库约束检查。
     */
    private void validateNoActiveReferences(Long materialId) {
        boolean referenced = bomDetailRepository.existsEffectiveBomByMaterialId(
                materialId, BomStatusEnum.EFFECTIVE.getStatus())
                || workOrderMaterialRepository.existsActiveOrderByMaterialId(
                        materialId, WorkOrderStatusEnum.activeStatuses());
        if (referenced) {
            throw new ServiceException(ProductionErrorCodeConstants.MATERIAL_ACTIVE_REFERENCE_EXISTS);
        }
    }

    /** 校验不存在任意历史引用；删除前必须保留所有仍可追溯的业务关系。 */
    private void validateNoAnyReferences(Long materialId) {
        if (hasAnyReferences(materialId)) {
            throw new ServiceException(ProductionErrorCodeConstants.MATERIAL_REFERENCE_EXISTS);
        }
    }

    /**
     * 判断物料是否存在任何会固定计量单位语义的业务引用。
     *
     * <p>按 BOM、工单物料、库存和齐套分析四类表分别使用 exists 查询，避免为了判断存在性加载大集合。
     */
    private boolean hasAnyReferences(Long materialId) {
        return bomDetailRepository.existsByMaterialIdAndDeletedFalse(materialId)
                || workOrderMaterialRepository.existsByMaterialIdAndDeletedFalse(materialId)
                || materialStockRepository.existsByMaterialIdAndDeletedFalse(materialId)
                || kitAnalysisRepository.existsByMaterialIdAndDeletedFalse(materialId);
    }

    /** 写锁校验单位存在且启用，防止单位在校验通过后被并发停用。 */
    private void requireEnabledUnitForUpdate(Long unitId) {
        if (!unitService.lockAndCheckEnabled(unitId)) {
            throw new ServiceException(ProductionErrorCodeConstants.UNIT_NOT_AVAILABLE);
        }
    }

    /**
     * 校验物料编码唯一。
     *
     * <p>更新时排除当前物料自身；应用层检查用于友好提示，数据库唯一键仍是并发场景的最终防线。
     */
    private void validateCode(String code, Long excludeId) {
        boolean exists = excludeId == null
                ? materialRepository.existsByMaterialCodeAndDeletedFalse(code)
                : materialRepository.existsByMaterialCodeAndIdNotAndDeletedFalse(code, excludeId);
        if (exists) {
            throw new ServiceException(ProductionErrorCodeConstants.MATERIAL_CODE_DUPLICATE);
        }
    }

    /** 校验物料类型，阻断未知枚举值进入主档表。 */
    private void validateType(Integer type) {
        if (!MaterialTypeEnum.contains(type)) {
            throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR, "物料类型不合法");
        }
    }

    /** 校验查询条件中的可选物料类型；未传值表示不按类型过滤。 */
    private void validateOptionalType(Integer type) {
        if (type != null) {
            validateType(type);
        }
    }

    /** 校验物料启停状态，只允许系统定义的启用和停用两种值。 */
    private void validateStatus(Integer status) {
        if (!CommonStatusEnum.ENABLED.getStatus().equals(status)
                && !CommonStatusEnum.DISABLED.getStatus().equals(status)) {
            throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR, "物料状态不合法");
        }
    }

    /** 查询未删除物料，供只读详情和非并发校验使用。 */
    private MaterialEntity require(Long id) {
        return materialRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.MATERIAL_NOT_EXISTS));
    }

    /** 写锁查询未删除物料，供更新、删除及状态切换建立稳定快照。 */
    private MaterialEntity requireForUpdate(Long id) {
        return materialRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.MATERIAL_NOT_EXISTS));
    }

    /** 校验前端携带的乐观锁版本，拒绝基于旧页面的覆盖式更新。 */
    private void validateVersion(MaterialEntity material, Integer expectedVersion) {
        if (!Objects.equals(material.getVersion(), expectedVersion)) {
            throw new ServiceException(ProductionErrorCodeConstants.MATERIAL_CONCURRENT_MODIFICATION);
        }
    }

    /**
     * 保存并转换唯一键和乐观锁异常。
     *
     * <p>立即 flush 让约束冲突在当前业务方法内暴露，并将基础设施异常转成前端可识别的业务错误。
     */
    private void save(MaterialEntity material) {
        try {
            materialRepository.saveAndFlush(material);
        } catch (DataIntegrityViolationException exception) {
            ProductionPersistenceExceptionTranslator.translateUniqueConstraint(
                    exception, MATERIAL_CODE_CONSTRAINT, ProductionErrorCodeConstants.MATERIAL_CODE_DUPLICATE);
        } catch (OptimisticLockingFailureException exception) {
            throw new ServiceException(ProductionErrorCodeConstants.MATERIAL_CONCURRENT_MODIFICATION);
        }
    }

    /** 规范化物料请求，统一编码大小写并将空规格折叠为 null。 */
    private void normalize(MaterialSaveReqVO reqVO) {
        reqVO.setMaterialCode(reqVO.getMaterialCode().trim().toUpperCase(Locale.ROOT));
        reqVO.setMaterialName(reqVO.getMaterialName().trim());
        reqVO.setSpec(StringUtils.hasText(reqVO.getSpec()) ? reqVO.getSpec().trim() : null);
    }

    /** 规范化页码，避免请求页码超过最后一页时生成非法偏移量。 */
    private int normalizePageNo(int requested, int pageSize, long total) {
        return Math.min(requested, (int) ((total + pageSize - 1) / pageSize));
    }
}
