package com.badminton.mes.module.equipment.service.impl;

import java.util.List;
import java.util.Set;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.equipment.constants.EquipmentErrorCodeConstants;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerSaveReqVO;
import com.badminton.mes.module.equipment.convert.EquipmentLedgerConvert;
import com.badminton.mes.module.equipment.dal.entity.EquipmentLedgerEntity;
import com.badminton.mes.module.equipment.dal.redis.EquipmentCache;
import com.badminton.mes.module.equipment.dal.redis.EquipmentRedisKeyConstants;
import com.badminton.mes.module.equipment.dal.repository.EquipmentCategoryRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentLedgerRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentLedgerSpecifications;
import com.badminton.mes.module.equipment.dal.repository.EquipmentMaintenancePlanRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentMaintenanceRecordRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentManufacturerRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentRepairOrderRepository;
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

    /** 逻辑删除编码保留前缀，用于释放原设备编码唯一约束 */
    private static final String DELETED_EQUIPMENT_CODE_PREFIX = "__DELETED_";

    /** 设备存在处理中的报修任务，不允许删除 */
    private static final Set<String> ACTIVE_REPAIR_STATUSES = Set.of("REPORTED", "ASSIGNED", "REPAIRING");

    /** 进行中的设备保养任务状态 */
    private static final Set<String> IN_PROGRESS_MAINTENANCE_STATUS = Set.of("IN_PROGRESS");

    private final EquipmentLedgerRepository ledgerRepository;

    private final EquipmentCategoryRepository categoryRepository;

    private final EquipmentManufacturerRepository manufacturerRepository;

    private final EquipmentRepairOrderRepository repairOrderRepository;

    private final EquipmentMaintenancePlanRepository maintenancePlanRepository;

    private final EquipmentMaintenanceRecordRepository maintenanceRecordRepository;

    private final EquipmentCache equipmentCache;

    /**
     * 构造器注入，保证依赖不可变。
     *
     * @param ledgerRepository      设备台账 Repository
     * @param categoryRepository    设备类别 Repository
     * @param manufacturerRepository 设备制造商 Repository
     * @param repairOrderRepository 设备报修任务 Repository
     * @param maintenancePlanRepository 设备保养计划 Repository
     * @param maintenanceRecordRepository 设备保养记录 Repository
     * @param equipmentCache 设备缓存组件
     */
    public EquipmentLedgerServiceImpl(EquipmentLedgerRepository ledgerRepository,
                                      EquipmentCategoryRepository categoryRepository,
                                      EquipmentManufacturerRepository manufacturerRepository,
                                      EquipmentRepairOrderRepository repairOrderRepository,
                                      EquipmentMaintenancePlanRepository maintenancePlanRepository,
                                      EquipmentMaintenanceRecordRepository maintenanceRecordRepository,
                                      EquipmentCache equipmentCache) {
        this.ledgerRepository = ledgerRepository;
        this.categoryRepository = categoryRepository;
        this.manufacturerRepository = manufacturerRepository;
        this.repairOrderRepository = repairOrderRepository;
        this.maintenancePlanRepository = maintenancePlanRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.equipmentCache = equipmentCache;
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
        EquipmentLedgerEntity existing = ledgerRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_LEDGER_NOT_EXISTS));
        validateEquipmentCode(reqVO.getEquipmentCode(), id);
        validateCategoryAvailable(reqVO.getCategoryId());
        validateManufacturerAvailable(reqVO.getManufacturerId());

        long inProgressMaintenanceCount =
                maintenanceRecordRepository.countByEquipmentIdAndRecordStatusInAndDeletedFalse(
                        id, IN_PROGRESS_MAINTENANCE_STATUS);
        String requestedEquipmentStatus = reqVO.getEquipmentStatus();
        if (inProgressMaintenanceCount > 0
                && requestedEquipmentStatus != null
                && !"MAINTAINING".equals(requestedEquipmentStatus)) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_STATUS_OPERATION_NOT_ALLOWED);
        }
        if (inProgressMaintenanceCount == 0 && "MAINTAINING".equals(requestedEquipmentStatus)) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_STATUS_OPERATION_NOT_ALLOWED);
        }

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
        evictLedgerCacheAfterCommit(id);
        logger.info("[修改设备台账] id: {}, equipmentCode: {}", id, reqVO.getEquipmentCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEquipmentLedger(Long id) {
        EquipmentLedgerEntity equipmentLedger = ledgerRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_LEDGER_NOT_EXISTS));
        if ("RUNNING".equals(equipmentLedger.getEquipmentStatus())) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_STATUS_OPERATION_NOT_ALLOWED);
        }

        long activeRepairOrderCount = repairOrderRepository.countByEquipmentIdAndRepairStatusInAndDeletedFalse(
                id, ACTIVE_REPAIR_STATUSES);
        if (activeRepairOrderCount > 0) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_LEDGER_HAS_REPAIR_ORDER);
        }

        long maintenancePlanCount = maintenancePlanRepository.countByEquipmentIdAndDeletedFalse(id);
        long maintenanceRecordCount = maintenanceRecordRepository.countByEquipmentIdAndDeletedFalse(id);
        if (maintenancePlanCount > 0 || maintenanceRecordCount > 0) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_LEDGER_HAS_MAINTENANCE);
        }

        String deletedCode = buildDeletedEquipmentCode(equipmentLedger.getId());
        if (ledgerRepository.existsByEquipmentCode(deletedCode)) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_LEDGER_CODE_DUPLICATE);
        }
        equipmentLedger.setEquipmentCode(deletedCode);
        equipmentLedger.setDeleted(true);
        ledgerRepository.save(equipmentLedger);
        evictLedgerCacheAfterCommit(id);

        logger.info("[删除设备台账] id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public EquipmentLedgerRespVO getEquipmentLedger(Long id) {
        return equipmentCache.getOrLoadDetail(EquipmentRedisKeyConstants.LEDGER_RESOURCE,
                id, EquipmentLedgerRespVO.class, () -> {
            EquipmentLedgerRespVO response = EquipmentLedgerConvert.toRespVO(validateLedgerExists(id));
            return response;
        });
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
     * <p>删除态编码使用业务请求不可占用的保留前缀，并包含设备主键，确保全表唯一且长度稳定。
     *
     * @param equipmentId 设备主键
     * @return 长度不超过 32 的删除态设备编码
     */
    private String buildDeletedEquipmentCode(Long equipmentId) {
        return DELETED_EQUIPMENT_CODE_PREFIX + Long.toString(equipmentId, 36).toUpperCase();
    }

    private void evictLedgerCacheAfterCommit(Long id) {
        equipmentCache.evictDetailAfterCommit(EquipmentRedisKeyConstants.LEDGER_RESOURCE, id);
    }
}
