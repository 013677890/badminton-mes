package com.badminton.mes.module.equipment.service.impl;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.equipment.constants.EquipmentErrorCodeConstants;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerSaveReqVO;
import com.badminton.mes.module.equipment.convert.EquipmentLedgerConvert;
import com.badminton.mes.module.equipment.dal.entity.EquipmentLedgerEntity;
import com.badminton.mes.module.equipment.dal.repository.EquipmentCategoryRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentLedgerRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentLedgerSpecifications;
import com.badminton.mes.module.equipment.dal.repository.EquipmentManufacturerRepository;
import com.badminton.mes.module.equipment.service.EquipmentLedgerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设备台账 Service 实现。
 *
 * <p>设备台账作为主数据，不在实体层建立跨表级联关系；类别、制造商等关联有效性由 Service 负责校验。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Service
public class EquipmentLedgerServiceImpl implements EquipmentLedgerService {

    private static final Logger logger = LoggerFactory.getLogger(EquipmentLedgerServiceImpl.class);

    /** TODO(角色C, 2026/07/09): 认证模块建设后，改为从登录上下文获取当前用户 id */
    private static final Long DEFAULT_OPERATOR_ID = 1L;

    /** 新建设备默认启用 */
    private static final int DEFAULT_ENABLED_STATUS = 1;

    /** 新建设备默认空闲 */
    private static final String DEFAULT_EQUIPMENT_STATUS = "IDLE";

    /** 设备编码数据库字段最大长度 */
    private static final int EQUIPMENT_CODE_MAX_LENGTH = 32;

    /** 逻辑删除编码后缀前缀，用于释放原设备编码唯一约束 */
    private static final String DELETED_CODE_SUFFIX_PREFIX = "_D";

    private final EquipmentLedgerRepository ledgerRepository;

    private final EquipmentCategoryRepository categoryRepository;

    private final EquipmentManufacturerRepository manufacturerRepository;

    /**
     * 构造器注入，保证依赖不可变。
     *
     * @param ledgerRepository       设备台账 Repository
     * @param categoryRepository     设备类别 Repository
     * @param manufacturerRepository 设备制造商 Repository
     */
    public EquipmentLedgerServiceImpl(EquipmentLedgerRepository ledgerRepository,
                                      EquipmentCategoryRepository categoryRepository,
                                      EquipmentManufacturerRepository manufacturerRepository) {
        this.ledgerRepository = ledgerRepository;
        this.categoryRepository = categoryRepository;
        this.manufacturerRepository = manufacturerRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEquipmentLedger(EquipmentLedgerSaveReqVO reqVO) {
        validateEquipmentCode(reqVO.getEquipmentCode(), null);
        validateCategoryAvailable(reqVO.getCategoryId());
        validateManufacturerAvailable(reqVO.getManufacturerId());

        EquipmentLedgerEntity equipmentLedger = EquipmentLedgerConvert.toEntity(reqVO);
        equipmentLedger.setCreateBy(DEFAULT_OPERATOR_ID);
        if (equipmentLedger.getStatus() == null) {
            equipmentLedger.setStatus(DEFAULT_ENABLED_STATUS);
        }
        if (equipmentLedger.getEquipmentStatus() == null) {
            equipmentLedger.setEquipmentStatus(DEFAULT_EQUIPMENT_STATUS);
        }

        try {
            ledgerRepository.saveAndFlush(equipmentLedger);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("uk_equipment_code")) {
                throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_LEDGER_CODE_DUPLICATE);
            }
            throw e;
        }

        logger.info("[创建设备台账] id: {}, equipmentCode: {}", equipmentLedger.getId(), equipmentLedger.getEquipmentCode());
        return equipmentLedger.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEquipmentLedger(Long id, EquipmentLedgerSaveReqVO reqVO) {
        EquipmentLedgerEntity existing = validateLedgerExists(id);
        validateEquipmentCode(reqVO.getEquipmentCode(), id);
        validateCategoryAvailable(reqVO.getCategoryId());
        validateManufacturerAvailable(reqVO.getManufacturerId());

        existing.setEquipmentCode(reqVO.getEquipmentCode());
        existing.setEquipmentName(reqVO.getEquipmentName());
        existing.setCategoryId(reqVO.getCategoryId());
        existing.setManufacturerId(reqVO.getManufacturerId());
        existing.setEquipmentModel(reqVO.getEquipmentModel());
        existing.setSerialNumber(reqVO.getSerialNumber());
        existing.setWorkshopId(reqVO.getWorkshopId());
        existing.setProductionLineId(reqVO.getProductionLineId());
        existing.setInstallationLocation(reqVO.getInstallationLocation());
        existing.setPurchaseDate(reqVO.getPurchaseDate());
        existing.setCommissioningDate(reqVO.getCommissioningDate());
        existing.setResponsiblePerson(reqVO.getResponsiblePerson());
        existing.setRemark(reqVO.getRemark());
        if (reqVO.getEquipmentStatus() != null) {
            existing.setEquipmentStatus(reqVO.getEquipmentStatus());
        }
        if (reqVO.getStatus() != null) {
            existing.setStatus(reqVO.getStatus());
        }

        ledgerRepository.save(existing);
        logger.info("[修改设备台账] id: {}, equipmentCode: {}", id, reqVO.getEquipmentCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEquipmentLedger(Long id) {
        EquipmentLedgerEntity equipmentLedger = validateLedgerExists(id);
        if ("RUNNING".equals(equipmentLedger.getEquipmentStatus())) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_STATUS_OPERATION_NOT_ALLOWED);
        }

        String deletedCode = buildDeletedEquipmentCode(equipmentLedger.getEquipmentCode(), equipmentLedger.getId());
        equipmentLedger.setEquipmentCode(deletedCode);
        equipmentLedger.setDeleted(true);
        ledgerRepository.save(equipmentLedger);

        logger.info("[删除设备台账] id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public EquipmentLedgerRespVO getEquipmentLedger(Long id) {
        EquipmentLedgerEntity equipmentLedger = validateLedgerExists(id);
        return EquipmentLedgerConvert.toRespVO(equipmentLedger);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<EquipmentLedgerRespVO> getEquipmentLedgerPage(EquipmentLedgerPageReqVO reqVO) {
        Specification<EquipmentLedgerEntity> specification = EquipmentLedgerSpecifications.page(reqVO);

        long total = ledgerRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        int pageSize = reqVO.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(reqVO.getPageNo(), totalPages);

        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<EquipmentLedgerEntity> page = ledgerRepository.findAll(specification, pageRequest);
        List<EquipmentLedgerEntity> list = page.getContent();

        return PageResult.of(EquipmentLedgerConvert.toRespVOList(list), total, pageNo, pageSize);
    }

    /**
     * 校验设备台账存在且未删除。
     *
     * @param id 设备主键
     * @return 设备台账实体
     */
    private EquipmentLedgerEntity validateLedgerExists(Long id) {
        return ledgerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_LEDGER_NOT_EXISTS));
    }

    /**
     * 校验设备编码唯一性。
     *
     * @param equipmentCode 设备编码
     * @param excludeId     排除的设备 id，创建时传 null
     */
    private void validateEquipmentCode(String equipmentCode, Long excludeId) {
        boolean exists = excludeId == null
                ? ledgerRepository.existsByEquipmentCodeAndDeletedFalse(equipmentCode)
                : ledgerRepository.existsByEquipmentCodeAndIdNotAndDeletedFalse(equipmentCode, excludeId);

        if (exists) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_LEDGER_CODE_DUPLICATE);
        }
    }

    /**
     * 校验设备类别存在且未删除。
     *
     * @param categoryId 设备类别 id
     */
    private void validateCategoryAvailable(Long categoryId) {
        boolean exists = categoryRepository.findByIdAndDeletedFalse(categoryId).isPresent();
        if (!exists) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_CATEGORY_NOT_EXISTS);
        }
    }

    /**
     * 校验设备制造商存在且未删除。
     *
     * @param manufacturerId 设备制造商 id，可空
     */
    private void validateManufacturerAvailable(Long manufacturerId) {
        if (manufacturerId == null) {
            return;
        }

        boolean exists = manufacturerRepository.findByIdAndDeletedFalse(manufacturerId).isPresent();
        if (!exists) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MANUFACTURER_NOT_EXISTS);
        }
    }

    /**
     * 构造逻辑删除后的设备编码。
     *
     * <p>设备编码字段长度为 32，删除时必须先截断原编码再拼接确定性短后缀，避免长编码删除失败。
     * 后缀包含设备主键，保证同一张表内逻辑删除编码稳定且不与其他删除记录冲突。
     *
     * @param originalEquipmentCode 原设备编码
     * @param equipmentId           设备主键
     * @return 长度不超过 32 的删除态设备编码
     */
    private String buildDeletedEquipmentCode(String originalEquipmentCode, Long equipmentId) {
        String deletedCodeSuffix = DELETED_CODE_SUFFIX_PREFIX + Long.toString(equipmentId, 36).toUpperCase();
        int preservedPrefixLength = EQUIPMENT_CODE_MAX_LENGTH - deletedCodeSuffix.length();
        if (preservedPrefixLength <= 0) {
            return deletedCodeSuffix.substring(0, EQUIPMENT_CODE_MAX_LENGTH);
        }

        String preservedOriginalCode = originalEquipmentCode.length() <= preservedPrefixLength
                ? originalEquipmentCode
                : originalEquipmentCode.substring(0, preservedPrefixLength);
        return preservedOriginalCode + deletedCodeSuffix;
    }
}
