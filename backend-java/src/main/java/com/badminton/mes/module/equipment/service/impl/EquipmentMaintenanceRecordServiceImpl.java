package com.badminton.mes.module.equipment.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.equipment.constants.EquipmentErrorCodeConstants;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentMaintenanceRecordSaveReqVO;
import com.badminton.mes.module.equipment.convert.EquipmentMaintenanceRecordConvert;
import com.badminton.mes.module.equipment.dal.entity.EquipmentLedgerEntity;
import com.badminton.mes.module.equipment.dal.entity.EquipmentMaintenancePlanEntity;
import com.badminton.mes.module.equipment.dal.entity.EquipmentMaintenanceRecordEntity;
import com.badminton.mes.module.equipment.dal.redis.EquipmentCache;
import com.badminton.mes.module.equipment.dal.redis.EquipmentRedisKeyConstants;
import com.badminton.mes.module.equipment.dal.repository.EquipmentLedgerRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentMaintenancePlanRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentMaintenanceRecordRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentMaintenanceRecordSpecifications;
import com.badminton.mes.module.equipment.service.EquipmentMaintenanceRecordService;
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
import org.springframework.util.StringUtils;

/**
 * 设备保养记录 Service 实现。
 *
 * <p>保养记录状态变化会同步设备台账状态：执行中置为维护中，完成或取消后恢复可用状态；
 * 完成记录还会回写计划最近完成时间。以上修改在同一事务内完成，并在提交后清理设备缓存。
 *
 * @author MES 开发组
 * @date 2026/07/16
 */
@Service
public class EquipmentMaintenanceRecordServiceImpl implements EquipmentMaintenanceRecordService {

    private static final Logger logger = LoggerFactory.getLogger(EquipmentMaintenanceRecordServiceImpl.class);
    private static final Long DEFAULT_OPERATOR_ID = 1L;
    private static final String PENDING_STATUS = "PENDING";
    private static final String IN_PROGRESS_STATUS = "IN_PROGRESS";
    private static final String COMPLETED_STATUS = "COMPLETED";
    private static final String CANCELLED_STATUS = "CANCELLED";
    private static final String IDLE_EQUIPMENT_STATUS = "IDLE";
    private static final String STOPPED_EQUIPMENT_STATUS = "STOPPED";
    private static final String MAINTAINING_EQUIPMENT_STATUS = "MAINTAINING";
    private static final int ENABLED_USER_STATUS = 1;
    private static final String DELETED_RECORD_NO_PREFIX = "__DELETED_";
    private static final DateTimeFormatter RECORD_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final EquipmentMaintenanceRecordRepository maintenanceRecordRepository;
    private final EquipmentMaintenancePlanRepository maintenancePlanRepository;
    private final EquipmentLedgerRepository ledgerRepository;
    private final UserRepository userRepository;
    private final EquipmentCache equipmentCache;

    public EquipmentMaintenanceRecordServiceImpl(EquipmentMaintenanceRecordRepository maintenanceRecordRepository,
                                                 EquipmentMaintenancePlanRepository maintenancePlanRepository,
                                                 EquipmentLedgerRepository ledgerRepository,
                                                 UserRepository userRepository,
                                                 EquipmentCache equipmentCache) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.maintenancePlanRepository = maintenancePlanRepository;
        this.ledgerRepository = ledgerRepository;
        this.userRepository = userRepository;
        this.equipmentCache = equipmentCache;
    }

    /** 创建保养记录，并根据初始状态同步设备和计划。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEquipmentMaintenanceRecord(EquipmentMaintenanceRecordSaveReqVO reqVO) {
        EquipmentMaintenancePlanEntity plan = validateEnabledPlanForUpdate(reqVO.getPlanId());
        validateEquipmentAvailableForUpdate(plan.getEquipmentId());
        validateEnabledUser(reqVO.getExecutorUserId());

        String requestedStatus = StringUtils.hasText(reqVO.getRecordStatus())
                ? reqVO.getRecordStatus()
                : PENDING_STATUS;
        if (!PENDING_STATUS.equals(requestedStatus)) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_STATUS_OPERATION_NOT_ALLOWED);
        }

        EquipmentMaintenanceRecordEntity record = EquipmentMaintenanceRecordConvert.toEntity(reqVO);
        record.setEquipmentId(plan.getEquipmentId());
        record.setRecordStatus(PENDING_STATUS);
        record.setMaintenanceResult(null);
        record.setAbnormalDescription(null);
        record.setStartTime(null);
        record.setFinishTime(null);
        record.setCreateBy(DEFAULT_OPERATOR_ID);
        if (!StringUtils.hasText(record.getRecordNo())) {
            record.setRecordNo(buildRecordNo());
        }
        validateRecordNo(record.getRecordNo(), null);
        saveRecord(record);

        logger.info("[创建设备保养记录] id: {}, recordNo: {}", record.getId(), record.getRecordNo());
        return record.getId();
    }

    /** 修改可编辑记录，校验状态迁移、执行时间和完成结果。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEquipmentMaintenanceRecord(Long id, EquipmentMaintenanceRecordSaveReqVO reqVO) {
        EquipmentMaintenanceRecordEntity existing = validateRecordExistsForUpdate(id);
        validateRecordIsMutable(existing);
        if (!existing.getPlanId().equals(reqVO.getPlanId())) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_STATUS_OPERATION_NOT_ALLOWED);
        }

        EquipmentMaintenancePlanEntity plan = validatePlanForUpdate(existing.getPlanId());
        EquipmentLedgerEntity equipment = validateEquipmentAvailableForUpdate(plan.getEquipmentId());
        Long executorUserId = reqVO.getExecutorUserId() != null
                ? reqVO.getExecutorUserId()
                : existing.getExecutorUserId();
        validateEnabledUser(executorUserId);

        String recordNo = StringUtils.hasText(reqVO.getRecordNo()) ? reqVO.getRecordNo() : existing.getRecordNo();
        validateRecordNo(recordNo, id);
        String previousStatus = existing.getRecordStatus();
        String nextStatus = StringUtils.hasText(reqVO.getRecordStatus()) ? reqVO.getRecordStatus() : previousStatus;
        validateStatusTransition(previousStatus, nextStatus);

        existing.setRecordNo(recordNo);
        existing.setScheduledTime(reqVO.getScheduledTime());
        existing.setMaintenanceContent(reqVO.getMaintenanceContent());
        existing.setRemark(reqVO.getRemark());
        if (reqVO.getExecutorUserId() != null) {
            existing.setExecutorUserId(reqVO.getExecutorUserId());
        }
        if (reqVO.getStartTime() != null) {
            existing.setStartTime(reqVO.getStartTime());
        }
        if (reqVO.getFinishTime() != null) {
            existing.setFinishTime(reqVO.getFinishTime());
        }
        if (StringUtils.hasText(reqVO.getMaintenanceResult())) {
            existing.setMaintenanceResult(reqVO.getMaintenanceResult());
        }
        if (reqVO.getAbnormalDescription() != null) {
            existing.setAbnormalDescription(reqVO.getAbnormalDescription());
        }
        existing.setRecordStatus(nextStatus);

        fillTimesByStatus(existing, previousStatus);
        if (CANCELLED_STATUS.equals(nextStatus) && !CANCELLED_STATUS.equals(previousStatus)) {
            clearCancelledResult(existing);
        }
        validateRecordTimes(existing);
        validateFieldsForStatus(existing);
        validateCompletionResult(existing);
        synchronizeEquipmentStatus(existing, equipment, previousStatus, nextStatus);
        saveRecord(existing);

        if (COMPLETED_STATUS.equals(nextStatus) && !COMPLETED_STATUS.equals(previousStatus)) {
            updatePlanAfterCompletion(plan);
        }
        logger.info("[修改设备保养记录] id: {}, recordNo: {}", id, existing.getRecordNo());
    }

    /** 逻辑删除未形成不可逆事实的保养记录。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEquipmentMaintenanceRecord(Long id) {
        EquipmentMaintenanceRecordEntity record = validateRecordExistsForUpdate(id);
        if (COMPLETED_STATUS.equals(record.getRecordStatus()) || CANCELLED_STATUS.equals(record.getRecordStatus())) {
            throw new ServiceException(
                    EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_TERMINAL_RECORD_IMMUTABLE);
        }
        if (!PENDING_STATUS.equals(record.getRecordStatus())) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_STATUS_OPERATION_NOT_ALLOWED);
        }

        String deletedRecordNo = buildDeletedRecordNo(record.getId());
        if (maintenanceRecordRepository.existsByRecordNo(deletedRecordNo)) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_RECORD_NO_DUPLICATE);
        }
        record.setRecordNo(deletedRecordNo);
        record.setDeleted(true);
        maintenanceRecordRepository.save(record);
        logger.info("[删除设备保养记录] id: {}", id);
    }

    /** 查询保养记录详情。 */
    @Override
    @Transactional(readOnly = true)
    public EquipmentMaintenanceRecordRespVO getEquipmentMaintenanceRecord(Long id) {
        return EquipmentMaintenanceRecordConvert.toRespVO(validateRecordExists(id));
    }

    /** 分页查询设备保养记录。 */
    @Override
    @Transactional(readOnly = true)
    public PageResult<EquipmentMaintenanceRecordRespVO> getEquipmentMaintenanceRecordPage(
            EquipmentMaintenanceRecordPageReqVO reqVO) {
        Specification<EquipmentMaintenanceRecordEntity> specification =
                EquipmentMaintenanceRecordSpecifications.page(reqVO);
        long total = maintenanceRecordRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        int pageSize = reqVO.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(reqVO.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "scheduledTime").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<EquipmentMaintenanceRecordEntity> page = maintenanceRecordRepository.findAll(specification, pageRequest);
        List<EquipmentMaintenanceRecordRespVO> responseList =
                EquipmentMaintenanceRecordConvert.toRespVOList(page.getContent());
        return PageResult.of(responseList, total, pageNo, pageSize);
    }

    private EquipmentMaintenanceRecordEntity validateRecordExists(Long id) {
        return maintenanceRecordRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(
                        EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_RECORD_NOT_EXISTS));
    }

    private EquipmentMaintenanceRecordEntity validateRecordExistsForUpdate(Long id) {
        return maintenanceRecordRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(
                        EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_RECORD_NOT_EXISTS));
    }

    private void validateRecordIsMutable(EquipmentMaintenanceRecordEntity record) {
        if (COMPLETED_STATUS.equals(record.getRecordStatus()) || CANCELLED_STATUS.equals(record.getRecordStatus())) {
            throw new ServiceException(
                    EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_TERMINAL_RECORD_IMMUTABLE);
        }
    }

    private EquipmentMaintenancePlanEntity validateEnabledPlanForUpdate(Long planId) {
        EquipmentMaintenancePlanEntity plan = validatePlanForUpdate(planId);
        if (!Integer.valueOf(1).equals(plan.getStatus())) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_PLAN_DISABLED);
        }
        return plan;
    }

    private EquipmentMaintenancePlanEntity validatePlanForUpdate(Long planId) {
        return maintenancePlanRepository.findByIdAndDeletedFalseForUpdate(planId)
                .orElseThrow(() -> new ServiceException(
                        EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_PLAN_NOT_EXISTS));
    }

    private EquipmentLedgerEntity validateEquipmentAvailableForUpdate(Long equipmentId) {
        EquipmentLedgerEntity equipment = ledgerRepository.findByIdAndDeletedFalseForUpdate(equipmentId)
                .orElseThrow(() -> new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_LEDGER_NOT_EXISTS));
        if ("SCRAPPED".equals(equipment.getEquipmentStatus())) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_STATUS_OPERATION_NOT_ALLOWED);
        }
        return equipment;
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

    private void validateStatusTransition(String previousStatus, String nextStatus) {
        if (previousStatus.equals(nextStatus)) {
            return;
        }
        Set<String> allowedStatuses = switch (previousStatus) {
            case PENDING_STATUS -> Set.of(IN_PROGRESS_STATUS, CANCELLED_STATUS);
            case IN_PROGRESS_STATUS -> Set.of(COMPLETED_STATUS, CANCELLED_STATUS);
            default -> Set.of();
        };
        if (!allowedStatuses.contains(nextStatus)) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_STATUS_OPERATION_NOT_ALLOWED);
        }
    }

    private void fillTimesByStatus(EquipmentMaintenanceRecordEntity record, String previousStatus) {
        boolean statusChanged = !previousStatus.equals(record.getRecordStatus());
        if (!statusChanged) {
            return;
        }

        LocalDateTime currentTime = LocalDateTime.now();
        if (IN_PROGRESS_STATUS.equals(record.getRecordStatus()) && record.getStartTime() == null) {
            record.setStartTime(currentTime);
        }
        if (COMPLETED_STATUS.equals(record.getRecordStatus())) {
            if (record.getStartTime() == null) {
                record.setStartTime(currentTime);
            }
            if (record.getFinishTime() == null) {
                record.setFinishTime(currentTime);
            }
        }
    }

    private void validateCompletionResult(EquipmentMaintenanceRecordEntity record) {
        if (!COMPLETED_STATUS.equals(record.getRecordStatus())) {
            return;
        }
        if (!StringUtils.hasText(record.getMaintenanceResult())) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_RESULT_REQUIRED);
        }
        if ("ABNORMAL".equals(record.getMaintenanceResult())
                && !StringUtils.hasText(record.getAbnormalDescription())) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_RESULT_REQUIRED);
        }
    }

    private void validateRecordTimes(EquipmentMaintenanceRecordEntity record) {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime startTime = record.getStartTime();
        LocalDateTime finishTime = record.getFinishTime();
        boolean startTimeInFuture = startTime != null && startTime.isAfter(currentTime);
        boolean finishTimeInFuture = finishTime != null && finishTime.isAfter(currentTime);
        boolean finishTimeBeforeStart = startTime != null && finishTime != null && finishTime.isBefore(startTime);
        if (startTimeInFuture || finishTimeInFuture || finishTimeBeforeStart) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_TIME_INVALID);
        }
    }

    private void validateFieldsForStatus(EquipmentMaintenanceRecordEntity record) {
        if (PENDING_STATUS.equals(record.getRecordStatus())) {
            boolean containsExecutionData = record.getStartTime() != null
                    || record.getFinishTime() != null
                    || StringUtils.hasText(record.getMaintenanceResult())
                    || StringUtils.hasText(record.getAbnormalDescription());
            if (containsExecutionData) {
                throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_TIME_INVALID);
            }
            return;
        }

        if (IN_PROGRESS_STATUS.equals(record.getRecordStatus())) {
            boolean containsCompletionData = record.getStartTime() == null
                    || record.getFinishTime() != null
                    || StringUtils.hasText(record.getMaintenanceResult())
                    || StringUtils.hasText(record.getAbnormalDescription());
            if (containsCompletionData) {
                throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_TIME_INVALID);
            }
            return;
        }

        if (COMPLETED_STATUS.equals(record.getRecordStatus())
                && (record.getStartTime() == null || record.getFinishTime() == null)) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_TIME_INVALID);
        }
    }

    /**
     * 根据保养记录状态同步设备台账；状态真正变化时才写库，减少无效更新和缓存抖动。
     */
    private void synchronizeEquipmentStatus(EquipmentMaintenanceRecordEntity record,
                                            EquipmentLedgerEntity equipment,
                                            String previousRecordStatus,
                                            String nextRecordStatus) {
        boolean startingMaintenance = PENDING_STATUS.equals(previousRecordStatus)
                && IN_PROGRESS_STATUS.equals(nextRecordStatus);
        if (startingMaintenance) {
            boolean equipmentCanEnterMaintenance = IDLE_EQUIPMENT_STATUS.equals(equipment.getEquipmentStatus())
                    || STOPPED_EQUIPMENT_STATUS.equals(equipment.getEquipmentStatus());
            long otherInProgressCount = maintenanceRecordRepository
                    .countByEquipmentIdAndRecordStatusAndIdNotAndDeletedFalse(
                            equipment.getId(), IN_PROGRESS_STATUS, record.getId());
            if (!equipmentCanEnterMaintenance || otherInProgressCount > 0) {
                throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_STATUS_OPERATION_NOT_ALLOWED);
            }
            record.setPreviousEquipmentStatus(equipment.getEquipmentStatus());
            equipment.setEquipmentStatus(MAINTAINING_EQUIPMENT_STATUS);
            ledgerRepository.save(equipment);
            evictEquipmentLedgerCacheAfterCommit(equipment.getId());
            return;
        }

        boolean endingMaintenance = IN_PROGRESS_STATUS.equals(previousRecordStatus)
                && (COMPLETED_STATUS.equals(nextRecordStatus) || CANCELLED_STATUS.equals(nextRecordStatus));
        if (!endingMaintenance) {
            return;
        }
        long otherInProgressCount = maintenanceRecordRepository
                .countByEquipmentIdAndRecordStatusAndIdNotAndDeletedFalse(
                        equipment.getId(), IN_PROGRESS_STATUS, record.getId());
        if (otherInProgressCount == 0 && MAINTAINING_EQUIPMENT_STATUS.equals(equipment.getEquipmentStatus())) {
            String restoredStatus = StringUtils.hasText(record.getPreviousEquipmentStatus())
                    ? record.getPreviousEquipmentStatus()
                    : IDLE_EQUIPMENT_STATUS;
            equipment.setEquipmentStatus(restoredStatus);
            ledgerRepository.save(equipment);
            evictEquipmentLedgerCacheAfterCommit(equipment.getId());
        }
    }

    private void evictEquipmentLedgerCacheAfterCommit(Long equipmentId) {
        equipmentCache.evictDetailAfterCommit(EquipmentRedisKeyConstants.LEDGER_RESOURCE, equipmentId);
    }

    private void clearCancelledResult(EquipmentMaintenanceRecordEntity record) {
        record.setFinishTime(null);
        record.setMaintenanceResult(null);
        record.setAbnormalDescription(null);
    }

    private void updatePlanAfterCompletion(EquipmentMaintenancePlanEntity plan) {
        LocalDateTime latestCompletedTime = maintenanceRecordRepository
                .findLatestCompletedTimeByPlanId(plan.getId())
                .orElseThrow(() -> new ServiceException(
                        EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_RECORD_NOT_EXISTS));
        plan.setLastMaintenanceTime(latestCompletedTime);
        plan.setNextMaintenanceTime(latestCompletedTime.plusDays(plan.getCycleDays()));
        maintenancePlanRepository.save(plan);
    }

    private void validateRecordNo(String recordNo, Long excludeId) {
        boolean exists = excludeId == null
                ? maintenanceRecordRepository.existsByRecordNoAndDeletedFalse(recordNo)
                : maintenanceRecordRepository.existsByRecordNoAndIdNotAndDeletedFalse(recordNo, excludeId);
        if (exists) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_RECORD_NO_DUPLICATE);
        }
    }

    private void saveRecord(EquipmentMaintenanceRecordEntity record) {
        try {
            maintenanceRecordRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("uk_maintenance_record_no")) {
                throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_RECORD_NO_DUPLICATE);
            }
            throw exception;
        }
    }

    private String buildRecordNo() {
        String timestamp = LocalDateTime.now().format(RECORD_NO_TIME_FORMATTER);
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "MNT-" + timestamp + "-" + randomSuffix;
    }

    private String buildDeletedRecordNo(Long recordId) {
        return DELETED_RECORD_NO_PREFIX + Long.toString(recordId, 36).toUpperCase();
    }
}
