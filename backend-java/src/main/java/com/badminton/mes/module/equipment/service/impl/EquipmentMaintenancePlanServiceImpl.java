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
 * 设备保养计划应用服务实现。
 *
 * <p>负责计划编码唯一性、设备与负责人可用性、历史任务引用保护和周期时间计算。写操作使用事务并
 * 对计划或设备读取加悲观写锁，使跨聚合校验所依据的数据在提交前保持稳定。
 */
@Service
public class EquipmentMaintenancePlanServiceImpl implements EquipmentMaintenancePlanService {

    /** 保养计划业务日志。 */
    private static final Logger logger = LoggerFactory.getLogger(EquipmentMaintenancePlanServiceImpl.class);

    /** 认证上下文接入前使用的临时操作人主键。 */
    private static final Long DEFAULT_OPERATOR_ID = 1L;

    /** 新建计划默认启用。 */
    private static final int DEFAULT_ENABLED_STATUS = 1;

    /** 客户端未指定时使用例行保养类型。 */
    private static final String DEFAULT_MAINTENANCE_TYPE = "ROUTINE";

    /** 逻辑删除编码保留前缀，用于释放原业务编码唯一约束。 */
    private static final String DELETED_PLAN_CODE_PREFIX = "__DELETED_";

    /** 可被选为计划负责人的系统用户启用状态。 */
    private static final int ENABLED_USER_STATUS = 1;

    /** 保养计划持久层，提供唯一性检查和悲观锁读取。 */
    private final EquipmentMaintenancePlanRepository maintenancePlanRepository;

    /** 保养记录持久层，用于历史引用保护和最近完成时间聚合。 */
    private final EquipmentMaintenanceRecordRepository maintenanceRecordRepository;

    /** 设备台账持久层，用于校验计划绑定设备并锁定其状态。 */
    private final EquipmentLedgerRepository ledgerRepository;

    /** 系统用户持久层，用于校验负责人存在且启用。 */
    private final UserRepository userRepository;

    /**
     * 构造保养计划服务并固定全部协作依赖。
     *
     * @param maintenancePlanRepository 保养计划持久层
     * @param maintenanceRecordRepository 保养记录持久层
     * @param ledgerRepository 设备台账持久层
     * @param userRepository 系统用户持久层
     */
    public EquipmentMaintenancePlanServiceImpl(EquipmentMaintenancePlanRepository maintenancePlanRepository,
                                               EquipmentMaintenanceRecordRepository maintenanceRecordRepository,
                                               EquipmentLedgerRepository ledgerRepository,
                                               UserRepository userRepository) {
        this.maintenancePlanRepository = maintenancePlanRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.ledgerRepository = ledgerRepository;
        this.userRepository = userRepository;
    }

    /**
     * 创建保养计划并在服务边界补齐业务默认值。
     *
     * <p>关联设备在校验时加写锁，避免设备被并发删除或报废；编码唯一索引在刷新时兜住并发创建。
     *
     * @param reqVO 计划创建数据
     * @return 新计划主键
     */
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

    /**
     * 更新计划并按已完成记录重新派生周期时间。
     *
     * <p>计划本身通过悲观锁串行化修改。已有记录时禁止换绑设备；已有完成记录时，最近和下次保养
     * 时间必须以数据库事实重新计算，而不是信任请求中的派生值。
     *
     * @param id 计划主键
     * @param reqVO 更新数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEquipmentMaintenancePlan(Long id, EquipmentMaintenancePlanSaveReqVO reqVO) {
        EquipmentMaintenancePlanEntity existing = validatePlanExistsForUpdate(id);
        validatePlanCode(reqVO.getPlanCode(), id);
        validateEquipmentAvailableForUpdate(reqVO.getEquipmentId());
        validateEnabledUser(reqVO.getResponsibleUserId());

        boolean equipmentChanged = !existing.getEquipmentId().equals(reqVO.getEquipmentId());
        // 历史记录固化了计划与设备的对应关系；允许迁移会破坏保养履历的可追溯性。
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
        // 已有完成记录时以真实完成时间为周期锚点，不能被客户端任意提交的下次时间覆盖。
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

    /**
     * 删除未被保养记录引用的计划。
     *
     * <p>锁定计划后执行引用保护检查，再用包含主键的保留编码释放原计划编码并逻辑删除。
     *
     * @param id 计划主键
     */
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

    /**
     * 查询有效计划详情，不触发周期重算。
     *
     * @param id 计划主键
     * @return 计划响应快照
     */
    @Override
    @Transactional(readOnly = true)
    public EquipmentMaintenancePlanRespVO getEquipmentMaintenancePlan(Long id) {
        return EquipmentMaintenancePlanConvert.toRespVO(validatePlanExists(id));
    }

    /**
     * 分页查询计划，空结果跳过列表查询，越界页码收敛到最后一页。
     *
     * @param reqVO 分页及筛选条件
     * @return 计划分页快照
     */
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
        // 将越界页归一化到最后一页，避免前端因数据删除得到有总数却无列表的响应。
        int pageNo = Math.min(reqVO.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize,
                Sort.by(Sort.Direction.ASC, "nextMaintenanceTime").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<EquipmentMaintenancePlanEntity> page = maintenancePlanRepository.findAll(specification, pageRequest);
        List<EquipmentMaintenancePlanRespVO> responseList = EquipmentMaintenancePlanConvert.toRespVOList(page.getContent());
        return PageResult.of(responseList, total, pageNo, pageSize);
    }

    /**
     * 查询未删除计划，用于无需写锁的详情读取。
     *
     * @param id 计划主键
     * @return 有效计划实体
     */
    private EquipmentMaintenancePlanEntity validatePlanExists(Long id) {
        return maintenancePlanRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_PLAN_NOT_EXISTS));
    }

    /**
     * 悲观锁定计划，使修改、删除及引用检查在同一事务中串行执行。
     *
     * @param id 计划主键
     * @return 已锁定的有效计划
     */
    private EquipmentMaintenancePlanEntity validatePlanExistsForUpdate(Long id) {
        return maintenancePlanRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_PLAN_NOT_EXISTS));
    }

    /**
     * 锁定并校验设备有效，报废设备不能承载新建或修改后的保养计划。
     *
     * @param equipmentId 设备主键
     * @return 已锁定设备
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
     * 执行计划编码应用层查重，更新场景排除当前计划。
     *
     * @param planCode 计划编码
     * @param excludeId 更新时排除的计划主键，创建时为空
     */
    private void validatePlanCode(String planCode, Long excludeId) {
        boolean exists = excludeId == null
                ? maintenancePlanRepository.existsByPlanCodeAndDeletedFalse(planCode)
                : maintenancePlanRepository.existsByPlanCodeAndIdNotAndDeletedFalse(planCode, excludeId);
        if (exists) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MAINTENANCE_PLAN_CODE_DUPLICATE);
        }
    }

    /**
     * 校验可选负责人存在、未删除且启用；空值表示计划暂未指定负责人。
     *
     * @param userId 负责人主键，可为空
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
     * 刷新计划并将目标唯一索引冲突转换为计划编码重复业务异常。
     *
     * <p>数据库约束是应用层查重在并发窗口下的最终兜底，其他完整性异常不得误判或吞掉。
     *
     * @param plan 待持久化计划
     */
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

    /**
     * 使用保留前缀和主键构造稳定删除态编码，避免依赖时间戳造成碰撞。
     *
     * @param planId 计划主键
     * @return 删除态计划编码
     */
    private String buildDeletedCode(Long planId) {
        return DELETED_PLAN_CODE_PREFIX + Long.toString(planId, 36).toUpperCase();
    }
}
