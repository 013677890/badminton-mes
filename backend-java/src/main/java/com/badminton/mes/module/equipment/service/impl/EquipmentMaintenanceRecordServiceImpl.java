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
 * 设备保养任务记录应用服务实现。
 *
 * <p>实现 PENDING -> IN_PROGRESS -> COMPLETED/CANCELLED 状态机，并在任务开始和结束时联动设备
 * 状态，在完成时重算计划周期。计划、设备和记录均通过悲观写锁参与同一事务，避免并发请求造成
 * 两条执行中任务、设备状态误恢复或计划时间丢失更新。
 */
@Service
public class EquipmentMaintenanceRecordServiceImpl implements EquipmentMaintenanceRecordService {

    /** 保养任务业务日志。 */
    private static final Logger logger = LoggerFactory.getLogger(EquipmentMaintenanceRecordServiceImpl.class);

    /** 认证上下文接入前使用的临时操作人主键。 */
    private static final Long DEFAULT_OPERATOR_ID = 1L;

    /** 状态机初始态：任务已排程但尚未执行。 */
    private static final String PENDING_STATUS = "PENDING";

    /** 状态机执行态：设备已切换为保养中。 */
    private static final String IN_PROGRESS_STATUS = "IN_PROGRESS";

    /** 状态机完成终态：已形成不可变保养履历。 */
    private static final String COMPLETED_STATUS = "COMPLETED";

    /** 状态机取消终态：保留任务但清除完成结果。 */
    private static final String CANCELLED_STATUS = "CANCELLED";

    /** 允许进入保养的设备空闲状态。 */
    private static final String IDLE_EQUIPMENT_STATUS = "IDLE";

    /** 允许进入保养的设备停机状态。 */
    private static final String STOPPED_EQUIPMENT_STATUS = "STOPPED";

    /** 保养执行期间设备统一使用的运行状态。 */
    private static final String MAINTAINING_EQUIPMENT_STATUS = "MAINTAINING";

    /** 可被选为执行人的系统用户启用状态。 */
    private static final int ENABLED_USER_STATUS = 1;

    /** 逻辑删除任务编号保留前缀，用于释放原唯一编码。 */
    private static final String DELETED_RECORD_NO_PREFIX = "__DELETED_";

    /** 自动任务编号中的秒级时间格式。 */
    private static final DateTimeFormatter RECORD_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 保养记录持久层，承担状态锁定、并发计数和唯一性兜底。 */
    private final EquipmentMaintenanceRecordRepository maintenanceRecordRepository;

    /** 保养计划持久层，用于锁定计划并回写周期时间。 */
    private final EquipmentMaintenancePlanRepository maintenancePlanRepository;

    /** 设备台账持久层，用于锁定并联动设备运行状态。 */
    private final EquipmentLedgerRepository ledgerRepository;

    /** 系统用户持久层，用于执行人可用性校验。 */
    private final UserRepository userRepository;

    /** 设备详情缓存，设备状态变更后在事务提交后失效。 */
    private final EquipmentCache equipmentCache;

    /**
     * 构造保养记录服务并固定状态机所需协作依赖。
     *
     * @param maintenanceRecordRepository 保养记录持久层
     * @param maintenancePlanRepository 保养计划持久层
     * @param ledgerRepository 设备台账持久层
     * @param userRepository 系统用户持久层
     * @param equipmentCache 设备详情缓存
     */
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

    /**
     * 在锁定计划和设备的事务中创建待处理任务。
     *
     * <p>转换器产生的执行结果、时间和客户端状态均不作为状态机初值，服务统一清空并固定为
     * {@code PENDING}；应用层查重用于快速失败，数据库唯一索引负责并发兜底。
     *
     * @param reqVO 保养任务创建数据
     * @return 新记录主键
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEquipmentMaintenanceRecord(EquipmentMaintenanceRecordSaveReqVO reqVO) {
        EquipmentMaintenancePlanEntity plan = validateEnabledPlanForUpdate(reqVO.getPlanId());
        validateEquipmentAvailableForUpdate(plan.getEquipmentId());
        validateEnabledUser(reqVO.getExecutorUserId());

        String requestedStatus = StringUtils.hasText(reqVO.getRecordStatus())
                ? reqVO.getRecordStatus()
                : PENDING_STATUS;
        // 创建是状态机唯一入口，禁止客户端绕过开始动作直接制造执行中或终态历史。
        if (!PENDING_STATUS.equals(requestedStatus)) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_STATUS_OPERATION_NOT_ALLOWED);
        }

        EquipmentMaintenanceRecordEntity record = EquipmentMaintenanceRecordConvert.toEntity(reqVO);
        // 设备从锁定后的计划派生，并清除客户端提前提交的执行态数据，确保新任务语义纯净。
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

    /**
     * 更新可变任务并协调记录状态、设备状态及计划周期。
     *
     * <p>先锁定记录，再锁定其原计划和设备，使状态迁移校验与联动写入处于同一事务快照；任务绑定
     * 的计划不可借更新请求替换。进入终态前会同时校验执行时间、结果字段和异常说明，避免仅状态
     * 合法但业务快照不完整的记录落库。
     *
     * @param id 保养记录主键
     * @param reqVO 更新数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEquipmentMaintenanceRecord(Long id, EquipmentMaintenanceRecordSaveReqVO reqVO) {
        EquipmentMaintenanceRecordEntity existing = validateRecordExistsForUpdate(id);
        validateRecordIsMutable(existing);
        // 保养履历必须始终指向原计划；跨计划迁移会同时破坏设备快照和周期统计。
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
        // 取消不是完成，遗留完成时间或结果会让历史报表产生错误结论。
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

    /**
     * 逻辑删除尚未开始的保养任务。
     *
     * <p>先通过悲观锁固定待删除记录状态，再将业务单号改写为由主键生成的保留值，既保留审计
     * 轨迹，也释放原唯一键供后续业务重新使用。执行中及终态记录禁止删除。
     *
     * @param id 保养记录主键
     */
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

    /**
     * 查询一条未删除记录并转换为对外快照。
     *
     * @param id 保养记录主键
     * @return 保养记录响应
     */
    @Override
    @Transactional(readOnly = true)
    public EquipmentMaintenanceRecordRespVO getEquipmentMaintenanceRecord(Long id) {
        return EquipmentMaintenanceRecordConvert.toRespVO(validateRecordExists(id));
    }

    /**
     * 按动态条件分页查询保养记录。
     *
     * <p>先计数可避免空结果时的列表查询；页码越界时收敛到最后一页，排序同时使用计划时间和主键，
     * 保证时间相同的数据仍具备稳定顺序。
     *
     * @param reqVO 分页及筛选条件
     * @return 保养记录分页快照
     */
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
        // 数据减少导致请求页越界时返回最后一页，保持分页元数据与列表一致。
        int pageNo = Math.min(reqVO.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "scheduledTime").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<EquipmentMaintenanceRecordEntity> page = maintenanceRecordRepository.findAll(specification, pageRequest);
        List<EquipmentMaintenanceRecordRespVO> responseList =
                EquipmentMaintenanceRecordConvert.toRespVOList(page.getContent());
        return PageResult.of(responseList, total, pageNo, pageSize);
    }

    /**
     * 查询未删除记录；用于只读场景，不申请数据库写锁。
     *
     * @param id 保养记录主键
     * @return 有效记录实体
     */
    private EquipmentMaintenanceRecordEntity validateRecordExists(Long id) {
        return maintenanceRecordRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(
                        EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_RECORD_NOT_EXISTS));
    }

    /**
     * 以悲观写锁读取记录，串行化针对同一任务的状态迁移和删除。
     *
     * @param id 保养记录主键
     * @return 已锁定的有效记录实体
     */
    private EquipmentMaintenanceRecordEntity validateRecordExistsForUpdate(Long id) {
        return maintenanceRecordRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(
                        EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_RECORD_NOT_EXISTS));
    }

    /**
     * 拒绝修改完成或取消终态，确保已经形成的执行审计快照不可回写。
     *
     * @param record 待更新记录
     */
    private void validateRecordIsMutable(EquipmentMaintenanceRecordEntity record) {
        if (COMPLETED_STATUS.equals(record.getRecordStatus()) || CANCELLED_STATUS.equals(record.getRecordStatus())) {
            throw new ServiceException(
                    EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_TERMINAL_RECORD_IMMUTABLE);
        }
    }

    /**
     * 锁定并校验创建任务所用计划处于启用状态。
     *
     * @param planId 保养计划主键
     * @return 已锁定的启用计划
     */
    private EquipmentMaintenancePlanEntity validateEnabledPlanForUpdate(Long planId) {
        EquipmentMaintenancePlanEntity plan = validatePlanForUpdate(planId);
        if (!Integer.valueOf(1).equals(plan.getStatus())) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_PLAN_DISABLED);
        }
        return plan;
    }

    /**
     * 以悲观写锁读取计划，防止任务更新期间计划被删除或并发修改周期。
     *
     * @param planId 保养计划主键
     * @return 已锁定的有效计划
     */
    private EquipmentMaintenancePlanEntity validatePlanForUpdate(Long planId) {
        return maintenancePlanRepository.findByIdAndDeletedFalseForUpdate(planId)
                .orElseThrow(() -> new ServiceException(
                        EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_PLAN_NOT_EXISTS));
    }

    /**
     * 以悲观写锁读取设备，并阻止报废设备参与新的保养状态联动。
     *
     * @param equipmentId 设备主键
     * @return 已锁定且可参与保养的设备
     */
    private EquipmentLedgerEntity validateEquipmentAvailableForUpdate(Long equipmentId) {
        EquipmentLedgerEntity equipment = ledgerRepository.findByIdAndDeletedFalseForUpdate(equipmentId)
                .orElseThrow(() -> new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_LEDGER_NOT_EXISTS));
        if ("SCRAPPED".equals(equipment.getEquipmentStatus())) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_STATUS_OPERATION_NOT_ALLOWED);
        }
        return equipment;
    }

    /**
     * 校验可选执行人存在、未删除且启用；空执行人表示任务暂未分派。
     *
     * @param userId 执行人主键，可为空
     */
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

    /**
     * 校验保养状态机的单向迁移。
     *
     * <p>待处理可开始或取消，执行中可完成或取消；相同状态允许保存字段，其他回退、跨越以及终态
     * 再迁移均被拒绝。
     *
     * @param previousStatus 当前持久化状态
     * @param nextStatus 请求目标状态
     */
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

    /**
     * 仅在状态首次变化时补齐执行时间，保留调用方已经提供的合法历史时间。
     *
     * @param record 待更新记录
     * @param previousStatus 更新前状态
     */
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

    /**
     * 校验完成态结果快照：必须有结果，异常结果还必须有异常说明。
     *
     * @param record 待校验记录
     */
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

    /**
     * 校验执行时间不在未来，且结束时间不早于开始时间。
     *
     * @param record 待校验记录
     */
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

    /**
     * 校验状态与字段快照相互匹配。
     *
     * <p>待处理不能提前携带执行数据，执行中必须只有开始时间，完成态必须同时具备开始和结束时间；
     * 由此防止绕过状态机直接写入自相矛盾的任务记录。
     *
     * @param record 待校验记录
     */
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
     * 根据任务状态迁移同步设备状态，并维护可恢复的设备原状态快照。
     *
     * <p>开始保养时，仅空闲或停机设备可切换为保养中，同时检查不存在其他执行中任务；结束时再次
     * 统计其他任务，只有当前记录是最后一个执行中任务且设备仍为保养中，才恢复开始时保存的状态。
     * 这种双向计数与设备悲观锁共同防止并发任务提前恢复设备。
     *
     * @param record 当前保养记录
     * @param equipment 已锁定的设备实体
     * @param previousRecordStatus 更新前记录状态
     * @param nextRecordStatus 更新后记录状态
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

    /**
     * 登记设备详情缓存在当前事务成功提交后失效，回滚时不污染缓存。
     *
     * @param equipmentId 设备主键
     */
    private void evictEquipmentLedgerCacheAfterCommit(Long equipmentId) {
        equipmentCache.evictDetailAfterCommit(EquipmentRedisKeyConstants.LEDGER_RESOURCE, equipmentId);
    }

    /**
     * 清除取消任务不应保留的完成结果，开始时间则保留为任务曾执行过的审计事实。
     *
     * @param record 转为取消态的记录
     */
    private void clearCancelledResult(EquipmentMaintenanceRecordEntity record) {
        record.setFinishTime(null);
        record.setMaintenanceResult(null);
        record.setAbnormalDescription(null);
    }

    /**
     * 以计划下所有已完成记录的最新完成时间重算计划周期。
     *
     * <p>不直接使用当前请求时间，可正确处理补录或并发完成后“最近完成记录”并非当前记录的情况。
     *
     * @param plan 待推进的已锁定计划
     */
    private void updatePlanAfterCompletion(EquipmentMaintenancePlanEntity plan) {
        LocalDateTime latestCompletedTime = maintenanceRecordRepository
                .findLatestCompletedTimeByPlanId(plan.getId())
                .orElseThrow(() -> new ServiceException(
                        EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_RECORD_NOT_EXISTS));
        plan.setLastMaintenanceTime(latestCompletedTime);
        plan.setNextMaintenanceTime(latestCompletedTime.plusDays(plan.getCycleDays()));
        maintenancePlanRepository.save(plan);
    }

    /**
     * 执行应用层保养单号查重，更新时排除记录自身。
     *
     * @param recordNo 保养单号
     * @param excludeId 更新时排除的记录主键，创建时为空
     */
    private void validateRecordNo(String recordNo, Long excludeId) {
        boolean exists = excludeId == null
                ? maintenanceRecordRepository.existsByRecordNoAndDeletedFalse(recordNo)
                : maintenanceRecordRepository.existsByRecordNoAndIdNotAndDeletedFalse(recordNo, excludeId);
        if (exists) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_RECORD_NO_DUPLICATE);
        }
    }

    /**
     * 立即刷新记录并将数据库唯一索引冲突翻译为稳定业务异常。
     *
     * <p>{@code saveAndFlush} 让约束在当前事务边界内暴露，作为并发请求同时通过应用层查重后的
     * 最终唯一性兜底；非目标约束异常保持原样上抛。
     *
     * @param record 待持久化记录
     */
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

    /**
     * 生成包含秒级时间和随机片段的保养单号，最终唯一性仍由数据库索引保证。
     *
     * @return 自动保养单号
     */
    private String buildRecordNo() {
        String timestamp = LocalDateTime.now().format(RECORD_NO_TIME_FORMATTER);
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "MNT-" + timestamp + "-" + randomSuffix;
    }

    /**
     * 使用记录主键生成稳定删除态单号，释放原业务单号且保留追溯标识。
     *
     * @param recordId 保养记录主键
     * @return 删除态保养单号
     */
    private String buildDeletedRecordNo(Long recordId) {
        return DELETED_RECORD_NO_PREFIX + Long.toString(recordId, 36).toUpperCase();
    }
}
