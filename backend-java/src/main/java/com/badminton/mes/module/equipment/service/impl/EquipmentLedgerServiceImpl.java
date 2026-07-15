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
 * <p>设备台账是保养、报修等设备业务的主数据边界。由于实体不建立跨表级联，服务在事务内显式
 * 校验类别和制造商，并在删除前保护处理中报修、保养计划及保养履历引用。设备状态还必须与执行中
 * 保养任务保持一致，禁止人工绕过任务状态机写入或清除 {@code MAINTAINING}。详情缓存只在写事务
 * 成功提交后失效。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Service
public class EquipmentLedgerServiceImpl implements EquipmentLedgerService {

    /** 记录设备台账关键写操作。 */
    private static final Logger logger = LoggerFactory.getLogger(EquipmentLedgerServiceImpl.class);

    /** TODO(角色C, 2026/07/09): 认证模块建设后，改为从登录上下文获取当前用户 id */
    private static final Long DEFAULT_OPERATOR_ID = 1L;

    /** 新建设备默认启用。 */
    private static final int DEFAULT_ENABLED_STATUS = 1;

    /** 新建设备默认空闲。 */
    private static final String DEFAULT_EQUIPMENT_STATUS = "IDLE";

    /** 逻辑删除编码保留前缀，用于释放原设备编码唯一约束。 */
    private static final String DELETED_EQUIPMENT_CODE_PREFIX = "__DELETED_";

    /** 仍占用设备业务生命周期、会阻止删除的报修状态集合。 */
    private static final Set<String> ACTIVE_REPAIR_STATUSES = Set.of("REPORTED", "ASSIGNED", "REPAIRING");

    /** 用于校验设备保养中状态真实性的执行中任务集合。 */
    private static final Set<String> IN_PROGRESS_MAINTENANCE_STATUS = Set.of("IN_PROGRESS");

    /** 台账仓储，提供悲观锁读取、编码唯一性检查和持久化。 */
    private final EquipmentLedgerRepository ledgerRepository;

    /** 类别仓储，用于校验设备能力分类引用。 */
    private final EquipmentCategoryRepository categoryRepository;

    /** 制造商仓储，用于校验可选制造商引用。 */
    private final EquipmentManufacturerRepository manufacturerRepository;

    /** 报修仓储，用于删除前检查处理中任务。 */
    private final EquipmentRepairOrderRepository repairOrderRepository;

    /** 保养计划仓储，用于保护设备计划引用。 */
    private final EquipmentMaintenancePlanRepository maintenancePlanRepository;

    /** 保养记录仓储，用于状态同步校验和履历引用保护。 */
    private final EquipmentMaintenanceRecordRepository maintenanceRecordRepository;

    /** 设备详情缓存协调器，写操作提交后负责失效。 */
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

    /**
     * 创建设备台账并补齐启用、空闲默认状态。
     *
     * <p>类别和可选制造商必须引用有效主数据；编码先应用层查重，再由立即刷新的数据库唯一索引
     * 封闭并发窗口。
     *
     * @param reqVO 设备创建数据
     * @return 新设备主键
     */
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

    /**
     * 锁定并更新设备台账，同时约束保养任务与设备状态一致。
     *
     * <p>存在执行中保养任务时只能保持保养中；没有执行中任务时又不得由普通台账更新伪造保养中。
     * 该双向校验确保设备状态只能由保养状态机取得和释放。
     *
     * @param id 设备主键
     * @param reqVO 设备更新数据
     */
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

    /**
     * 对设备加悲观写锁，并在全部状态与引用保护通过后逻辑删除。
     *
     * <p>运行中设备、处理中报修以及任何保养计划或履历均会阻止删除。删除态编码由主键稳定生成，
     * 在保留历史记录的同时释放原设备编码。
     *
     * @param id 设备主键
     */
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

    /**
     * 读取设备详情，缓存未命中时加载未删除台账并生成响应快照。
     *
     * @param id 设备主键
     * @return 设备详情快照
     */
    @Override
    @Transactional(readOnly = true)
    public EquipmentLedgerRespVO getEquipmentLedger(Long id) {
        return equipmentCache.getOrLoadDetail(EquipmentRedisKeyConstants.LEDGER_RESOURCE,
                id, EquipmentLedgerRespVO.class, () -> {
            EquipmentLedgerRespVO response = EquipmentLedgerConvert.toRespVO(validateLedgerExists(id));
            return response;
        });
    }

    /**
     * 分页查询设备；空结果跳过列表查询，越界页码收敛到最后一页。
     *
     * @param reqVO 分页及筛选条件
     * @return 设备分页快照
     */
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
     * 校验有效设备编码唯一性，更新场景排除当前设备。
     *
     * <p>该查询用于快速失败，数据库唯一索引仍是并发创建或修改时的最终一致性兜底。
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

    /**
     * 登记设备详情缓存在当前事务成功提交后失效。
     *
     * @param id 设备主键
     */
    private void evictLedgerCacheAfterCommit(Long id) {
        equipmentCache.evictDetailAfterCommit(EquipmentRedisKeyConstants.LEDGER_RESOURCE, id);
    }
}
