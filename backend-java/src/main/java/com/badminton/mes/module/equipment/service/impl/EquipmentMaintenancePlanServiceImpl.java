package com.badminton.mes.module.equipment.service.impl;

import java.util.List;
import java.time.LocalDateTime;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.equipment.constants.EquipmentErrorCodeConstants;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenancePlanPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenancePlanRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenancePlanSaveReqVO;
import com.badminton.mes.module.equipment.convert.EquipmentMaintenancePlanConvert;
import com.badminton.mes.module.equipment.dal.entity.EquipmentLedgerEntity;
import com.badminton.mes.module.equipment.dal.entity.EquipmentMaintenancePlanEntity;
import com.badminton.mes.module.equipment.dal.repository.EquipmentLedgerRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentMaintenancePlanRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentMaintenancePlanSpecifications;
import com.badminton.mes.module.equipment.dal.repository.EquipmentMaintenanceRecordRepository;
import com.badminton.mes.module.equipment.service.EquipmentMaintenancePlanService;
import com.badminton.mes.module.system.dal.entity.UserEntity;
import com.badminton.mes.module.system.dal.repository.UserRepository;

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
 * 设备保养计划 Service 实现。
 *
 * <p>Controller 通过接口调用本类。写方法在事务内锁定计划、设备和责任人相关数据，
 * 避免计划保存期间设备被报废或责任人被停用；删除采用逻辑删除保留保养追溯链路。
 *
 * @author MES 开发组
 * @date 2026/07/16
 */
@Service
public class EquipmentMaintenancePlanServiceImpl implements EquipmentMaintenancePlanService {

    private static final Logger logger = LoggerFactory.getLogger(EquipmentMaintenancePlanServiceImpl.class);
    private static final Long DEFAULT_OPERATOR_ID = 1L;
    private static final int DEFAULT_ENABLED_STATUS = 1;
    private static final String DEFAULT_MAINTENANCE_TYPE = "ROUTINE";
    private static final String DELETED_PLAN_CODE_PREFIX = "__DELETED_";
    private static final int ENABLED_USER_STATUS = 1;

    private final EquipmentMaintenancePlanRepository maintenancePlanRepository;
    private final EquipmentMaintenanceRecordRepository maintenanceRecordRepository;
    private final EquipmentLedgerRepository ledgerRepository;
    private final UserRepository userRepository;

    public EquipmentMaintenancePlanServiceImpl(EquipmentMaintenancePlanRepository maintenancePlanRepository,
                                               EquipmentMaintenanceRecordRepository maintenanceRecordRepository,
                                               EquipmentLedgerRepository ledgerRepository,
                                               UserRepository userRepository) {
        this.maintenancePlanRepository = maintenancePlanRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.ledgerRepository = ledgerRepository;
        this.userRepository = userRepository;
    }

    /** 创建设备保养计划并校验设备、责任人和计划编码。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEquipmentMaintenancePlan(EquipmentMaintenancePlanSaveReqVO reqVO) {
        validatePlanCode(reqVO.getPlanCode(), null);
        validateEquipmentAvailableForUpdate(reqVO.getEquipmentId());
        validateEnabledUser(reqVO.getResponsibleUserId());

        EquipmentMaintenancePlanEntity plan = EquipmentMaintenancePlanConvert.toEntity(reqVO);
        plan.setCreateBy(DEFAULT_OPERATOR_ID);
        if (plan.getMaintenanceType() == null) {
            plan.setMaintenanceType(DEFAULT_MAINTENANCE_TYPE);
        }
        if (plan.getStatus() == null) {
            plan.setStatus(DEFAULT_ENABLED_STATUS);
        }
        savePlan(plan);
        logger.info("[创建设备保养计划] id: {}, planCode: {}", plan.getId(), plan.getPlanCode());
        return plan.getId();
    }

    /** 修改保养计划，已有执行记录时保护不应改变的关联关系。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEquipmentMaintenancePlan(Long id, EquipmentMaintenancePlanSaveReqVO reqVO) {
        EquipmentMaintenancePlanEntity existing = validatePlanExistsForUpdate(id);
        validatePlanCode(reqVO.getPlanCode(), id);
        validateEquipmentAvailableForUpdate(reqVO.getEquipmentId());
        validateEnabledUser(reqVO.getResponsibleUserId());

        boolean equipmentChanged = !existing.getEquipmentId().equals(reqVO.getEquipmentId());
        if (equipmentChanged && maintenanceRecordRepository.countByPlanIdAndDeletedFalse(id) > 0) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_PLAN_HAS_ACTIVE_RECORD);
        }

        existing.setPlanCode(reqVO.getPlanCode());
        existing.setPlanName(reqVO.getPlanName());
        existing.setEquipmentId(reqVO.getEquipmentId());
        existing.setCycleDays(reqVO.getCycleDays());
        existing.setMaintenanceContent(reqVO.getMaintenanceContent());
        existing.setResponsibleUserId(reqVO.getResponsibleUserId());
        LocalDateTime latestCompletedTime = maintenanceRecordRepository.findLatestCompletedTimeByPlanId(id)
                .orElse(null);
        if (latestCompletedTime == null) {
            existing.setNextMaintenanceTime(reqVO.getNextMaintenanceTime());
        } else {
            existing.setLastMaintenanceTime(latestCompletedTime);
            existing.setNextMaintenanceTime(latestCompletedTime.plusDays(reqVO.getCycleDays()));
        }
        existing.setRemark(reqVO.getRemark());
        if (reqVO.getMaintenanceType() != null) {
            existing.setMaintenanceType(reqVO.getMaintenanceType());
        }
        if (reqVO.getStatus() != null) {
            existing.setStatus(reqVO.getStatus());
        }
        savePlan(existing);
        logger.info("[修改设备保养计划] id: {}, planCode: {}", id, existing.getPlanCode());
    }

    /** 在业务规则允许时逻辑删除计划，并改写唯一编码释放占用。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEquipmentMaintenancePlan(Long id) {
        EquipmentMaintenancePlanEntity plan = validatePlanExistsForUpdate(id);
        if (maintenanceRecordRepository.countByPlanIdAndDeletedFalse(id) > 0) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_PLAN_HAS_ACTIVE_RECORD);
        }

        String deletedPlanCode = buildDeletedCode(plan.getId());
        if (maintenancePlanRepository.existsByPlanCode(deletedPlanCode)) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_PLAN_CODE_DUPLICATE);
        }
        plan.setPlanCode(deletedPlanCode);
        plan.setDeleted(true);
        savePlan(plan);
        logger.info("[删除设备保养计划] id: {}", id);
    }

    /** 查询单条保养计划详情。 */
    @Override
    @Transactional(readOnly = true)
    public EquipmentMaintenancePlanRespVO getEquipmentMaintenancePlan(Long id) {
        return EquipmentMaintenancePlanConvert.toRespVO(validatePlanExists(id));
    }

    /** 分页查询保养计划，并将越界页码收敛到最后一页。 */
    @Override
    @Transactional(readOnly = true)
    public PageResult<EquipmentMaintenancePlanRespVO> getEquipmentMaintenancePlanPage(
            EquipmentMaintenancePlanPageReqVO reqVO) {
        Specification<EquipmentMaintenancePlanEntity> specification = EquipmentMaintenancePlanSpecifications.page(reqVO);
        long total = maintenancePlanRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        int pageSize = reqVO.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(reqVO.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize,
                Sort.by(Sort.Direction.ASC, "nextMaintenanceTime").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<EquipmentMaintenancePlanEntity> page = maintenancePlanRepository.findAll(specification, pageRequest);
        List<EquipmentMaintenancePlanRespVO> responseList = EquipmentMaintenancePlanConvert.toRespVOList(page.getContent());
        return PageResult.of(responseList, total, pageNo, pageSize);
    }

    private EquipmentMaintenancePlanEntity validatePlanExists(Long id) {
        return maintenancePlanRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_PLAN_NOT_EXISTS));
    }

    private EquipmentMaintenancePlanEntity validatePlanExistsForUpdate(Long id) {
        return maintenancePlanRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_PLAN_NOT_EXISTS));
    }

    private EquipmentLedgerEntity validateEquipmentAvailableForUpdate(Long equipmentId) {
        EquipmentLedgerEntity equipment = ledgerRepository.findByIdAndDeletedFalseForUpdate(equipmentId)
                .orElseThrow(() -> new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_LEDGER_NOT_EXISTS));
        if ("SCRAPPED".equals(equipment.getEquipmentStatus())) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_STATUS_OPERATION_NOT_ALLOWED);
        }
        return equipment;
    }

    private void validatePlanCode(String planCode, Long excludeId) {
        boolean exists = excludeId == null
                ? maintenancePlanRepository.existsByPlanCodeAndDeletedFalse(planCode)
                : maintenancePlanRepository.existsByPlanCodeAndIdNotAndDeletedFalse(planCode, excludeId);
        if (exists) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_PLAN_CODE_DUPLICATE);
        }
    }

    private void validateEnabledUser(Long userId) {
        if (userId == null) {
            return;
        }
        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ServiceException(
                        EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_USER_NOT_EXISTS));
        if (!Integer.valueOf(ENABLED_USER_STATUS).equals(user.getStatus())) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_USER_NOT_EXISTS);
        }
    }

    private void savePlan(EquipmentMaintenancePlanEntity plan) {
        try {
            maintenancePlanRepository.saveAndFlush(plan);
        } catch (DataIntegrityViolationException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("uk_maintenance_plan_code")) {
                throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_PLAN_CODE_DUPLICATE);
            }
            throw exception;
        }
    }

    private String buildDeletedCode(Long planId) {
        return DELETED_PLAN_CODE_PREFIX + Long.toString(planId, 36).toUpperCase();
    }
}
