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
        normalize(reqVO);
        validateType(reqVO.getMaterialType());
        validateStatus(reqVO.getStatus());
        validateCode(reqVO.getMaterialCode(), null);
        requireEnabledUnitForUpdate(reqVO.getUnitId());
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        MaterialEntity material = ProductionMasterDataConvert.toMaterialEntity(reqVO);
        material.setCreateBy(operatorId);
        material.setUpdateBy(operatorId);
        save(material);
        return material.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMaterial(Long id, MaterialUpdateReqVO reqVO) {
        normalize(reqVO);
        validateType(reqVO.getMaterialType());
        validateStatus(reqVO.getStatus());
        MaterialEntity material = requireForUpdate(id);
        validateVersion(material, reqVO.getVersion());
        validateCode(reqVO.getMaterialCode(), id);
        requireEnabledUnitForUpdate(reqVO.getUnitId());
        if (!Objects.equals(material.getUnitId(), reqVO.getUnitId()) && hasAnyReferences(id)) {
            throw new ServiceException(ProductionErrorCodeConstants.MATERIAL_UNIT_IMMUTABLE);
        }
        if (CommonStatusEnum.DISABLED.getStatus().equals(reqVO.getStatus())
                && !CommonStatusEnum.DISABLED.getStatus().equals(material.getStatus())) {
            validateNoActiveReferences(id);
        }
        ProductionMasterDataConvert.copyMaterial(reqVO, material);
        material.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        save(material);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMaterial(Long id, Integer version) {
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
        validateStatus(reqVO.getStatus());
        MaterialEntity material = requireForUpdate(id);
        validateVersion(material, reqVO.getVersion());
        if (Objects.equals(material.getStatus(), reqVO.getStatus())) {
            return;
        }
        if (CommonStatusEnum.ENABLED.getStatus().equals(reqVO.getStatus())) {
            requireEnabledUnitForUpdate(material.getUnitId());
        } else {
            validateNoActiveReferences(id);
        }
        material.setStatus(reqVO.getStatus());
        material.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        save(material);
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialRespVO getMaterial(Long id) {
        return ProductionMasterDataConvert.toMaterialRespVO(require(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MaterialRespVO> getMaterialPage(MaterialPageReqVO reqVO) {
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

    /** 校验不存在生效 BOM 或未结束工单引用。 */
    private void validateNoActiveReferences(Long materialId) {
        boolean referenced = bomDetailRepository.existsEffectiveBomByMaterialId(
                materialId, BomStatusEnum.EFFECTIVE.getStatus())
                || workOrderMaterialRepository.existsActiveOrderByMaterialId(
                        materialId, WorkOrderStatusEnum.activeStatuses());
        if (referenced) {
            throw new ServiceException(ProductionErrorCodeConstants.MATERIAL_ACTIVE_REFERENCE_EXISTS);
        }
    }

    /** 校验不存在任意历史引用。 */
    private void validateNoAnyReferences(Long materialId) {
        if (hasAnyReferences(materialId)) {
            throw new ServiceException(ProductionErrorCodeConstants.MATERIAL_REFERENCE_EXISTS);
        }
    }

    /** 判断物料是否存在任何会固定计量单位语义的业务引用。 */
    private boolean hasAnyReferences(Long materialId) {
        return bomDetailRepository.existsByMaterialIdAndDeletedFalse(materialId)
                || workOrderMaterialRepository.existsByMaterialIdAndDeletedFalse(materialId)
                || materialStockRepository.existsByMaterialIdAndDeletedFalse(materialId)
                || kitAnalysisRepository.existsByMaterialIdAndDeletedFalse(materialId);
    }

    /** 写锁校验单位存在且启用。 */
    private void requireEnabledUnitForUpdate(Long unitId) {
        if (!unitService.lockAndCheckEnabled(unitId)) {
            throw new ServiceException(ProductionErrorCodeConstants.UNIT_NOT_AVAILABLE);
        }
    }

    /** 校验物料编码唯一。 */
    private void validateCode(String code, Long excludeId) {
        boolean exists = excludeId == null
                ? materialRepository.existsByMaterialCodeAndDeletedFalse(code)
                : materialRepository.existsByMaterialCodeAndIdNotAndDeletedFalse(code, excludeId);
        if (exists) {
            throw new ServiceException(ProductionErrorCodeConstants.MATERIAL_CODE_DUPLICATE);
        }
    }

    /** 校验物料类型。 */
    private void validateType(Integer type) {
        if (!MaterialTypeEnum.contains(type)) {
            throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR, "物料类型不合法");
        }
    }

    /** 校验可选物料类型。 */
    private void validateOptionalType(Integer type) {
        if (type != null) {
            validateType(type);
        }
    }

    /** 校验物料启停状态。 */
    private void validateStatus(Integer status) {
        if (!CommonStatusEnum.ENABLED.getStatus().equals(status)
                && !CommonStatusEnum.DISABLED.getStatus().equals(status)) {
            throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR, "物料状态不合法");
        }
    }

    /** 查询未删除物料。 */
    private MaterialEntity require(Long id) {
        return materialRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.MATERIAL_NOT_EXISTS));
    }

    /** 写锁查询未删除物料。 */
    private MaterialEntity requireForUpdate(Long id) {
        return materialRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.MATERIAL_NOT_EXISTS));
    }

    /** 校验预期版本。 */
    private void validateVersion(MaterialEntity material, Integer expectedVersion) {
        if (!Objects.equals(material.getVersion(), expectedVersion)) {
            throw new ServiceException(ProductionErrorCodeConstants.MATERIAL_CONCURRENT_MODIFICATION);
        }
    }

    /** 保存并转换唯一键和乐观锁异常。 */
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

    /** 规范化物料请求。 */
    private void normalize(MaterialSaveReqVO reqVO) {
        reqVO.setMaterialCode(reqVO.getMaterialCode().trim().toUpperCase(Locale.ROOT));
        reqVO.setMaterialName(reqVO.getMaterialName().trim());
        reqVO.setSpec(StringUtils.hasText(reqVO.getSpec()) ? reqVO.getSpec().trim() : null);
    }

    /** 规范化页码。 */
    private int normalizePageNo(int requested, int pageSize, long total) {
        return Math.min(requested, (int) ((total + pageSize - 1) / pageSize));
    }
}
