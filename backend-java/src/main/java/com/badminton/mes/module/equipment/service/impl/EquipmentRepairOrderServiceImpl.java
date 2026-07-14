package com.badminton.mes.module.equipment.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.equipment.constants.EquipmentErrorCodeConstants;
import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderSaveReqVO;
import com.badminton.mes.module.equipment.convert.EquipmentRepairOrderConvert;
import com.badminton.mes.module.equipment.dal.entity.EquipmentFaultPrincipleEntity;
import com.badminton.mes.module.equipment.dal.entity.EquipmentLedgerEntity;
import com.badminton.mes.module.equipment.dal.entity.EquipmentRepairOrderEntity;
import com.badminton.mes.module.equipment.dal.repository.EquipmentFaultPrincipleRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentLedgerRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentRepairOrderRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentRepairOrderSpecifications;
import com.badminton.mes.module.equipment.service.EquipmentRepairOrderService;

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
 * 设备报修任务 Service 实现。
 *
 * <p>报修任务围绕设备台账建立维修闭环，课设阶段用人工录入方式模拟报修、派工、维修中和完成状态。
 *
 * @author 角色C
 * @date 2026/07/10
 */
@Service
public class EquipmentRepairOrderServiceImpl implements EquipmentRepairOrderService {

    private static final Logger logger = LoggerFactory.getLogger(EquipmentRepairOrderServiceImpl.class);

    /** TODO(角色C, 2026/07/10): 认证模块建设后，改为从登录上下文获取当前用户 id */
    private static final Long DEFAULT_OPERATOR_ID = 1L;

    /** 新建报修单默认状态 */
    private static final String DEFAULT_REPAIR_STATUS = "REPORTED";

    /** 维修中状态 */
    private static final String REPAIRING_STATUS = "REPAIRING";

    /** 已完成状态 */
    private static final String FINISHED_STATUS = "FINISHED";

    /** 已取消状态 */
    private static final String CANCELLED_STATUS = "CANCELLED";

    /** 报修单号数据库字段最大长度 */
    private static final int REPAIR_NO_MAX_LENGTH = 32;

    /** 逻辑删除单号后缀前缀，用于释放原报修单号唯一约束 */
    private static final String DELETED_REPAIR_NO_SUFFIX_PREFIX = "_D";

    /** 报修单号前缀 */
    private static final String REPAIR_NO_PREFIX = "REP";

    /** 报修单号日期格式 */
    private static final DateTimeFormatter REPAIR_NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final EquipmentRepairOrderRepository repairOrderRepository;

    private final EquipmentLedgerRepository ledgerRepository;

    private final EquipmentFaultPrincipleRepository faultPrincipleRepository;

    /**
     * 构造器注入，保证依赖不可变。
     *
     * @param repairOrderRepository   设备报修任务 Repository
     * @param ledgerRepository        设备台账 Repository
     * @param faultPrincipleRepository 设备故障原理 Repository
     */
    public EquipmentRepairOrderServiceImpl(EquipmentRepairOrderRepository repairOrderRepository,
                                           EquipmentLedgerRepository ledgerRepository,
                                           EquipmentFaultPrincipleRepository faultPrincipleRepository) {
        this.repairOrderRepository = repairOrderRepository;
        this.ledgerRepository = ledgerRepository;
        this.faultPrincipleRepository = faultPrincipleRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEquipmentRepairOrder(EquipmentRepairOrderSaveReqVO reqVO) {
        EquipmentLedgerEntity equipmentLedger = validateEquipmentAvailableForUpdate(reqVO.getEquipmentId());
        validateFaultPrincipleAvailableForEquipment(reqVO.getFaultPrincipleId(), equipmentLedger);

        EquipmentRepairOrderEntity repairOrder = EquipmentRepairOrderConvert.toEntity(reqVO);
        repairOrder.setCreateBy(DEFAULT_OPERATOR_ID);
        if (!StringUtils.hasText(repairOrder.getRepairNo())) {
            repairOrder.setRepairNo(buildRepairNo());
        }
        validateRepairNo(repairOrder.getRepairNo(), null);
        if (repairOrder.getReportTime() == null) {
            repairOrder.setReportTime(LocalDateTime.now());
        }
        if (repairOrder.getReportUserId() == null) {
            repairOrder.setReportUserId(DEFAULT_OPERATOR_ID);
        }
        if (repairOrder.getRepairStatus() == null) {
            repairOrder.setRepairStatus(DEFAULT_REPAIR_STATUS);
        }
        fillTimeByRepairStatus(repairOrder);

        try {
            repairOrderRepository.saveAndFlush(repairOrder);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateRepairNoException(e)) {
                throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_REPAIR_ORDER_NO_DUPLICATE);
            }
            throw e;
        }

        logger.info("[创建设备报修任务] id: {}, repairNo: {}", repairOrder.getId(), repairOrder.getRepairNo());
        return repairOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEquipmentRepairOrder(Long id, EquipmentRepairOrderSaveReqVO reqVO) {
        EquipmentRepairOrderEntity existing = validateRepairOrderExists(id);
        EquipmentLedgerEntity equipmentLedger = validateEquipmentAvailableForUpdate(reqVO.getEquipmentId());
        validateFaultPrincipleAvailableForEquipment(reqVO.getFaultPrincipleId(), equipmentLedger);

        String repairNo = StringUtils.hasText(reqVO.getRepairNo()) ? reqVO.getRepairNo() : existing.getRepairNo();
        validateRepairNo(repairNo, id);
        String previousRepairStatus = existing.getRepairStatus();
        String nextRepairStatus = reqVO.getRepairStatus() == null ? previousRepairStatus : reqVO.getRepairStatus();
        validateRepairStatusTransition(previousRepairStatus, nextRepairStatus);

        existing.setRepairNo(repairNo);
        existing.setEquipmentId(reqVO.getEquipmentId());
        existing.setFaultPrincipleId(reqVO.getFaultPrincipleId());
        existing.setFaultDescription(reqVO.getFaultDescription());
        if (reqVO.getReportTime() != null) {
            existing.setReportTime(reqVO.getReportTime());
        }
        if (reqVO.getReportUserId() != null) {
            existing.setReportUserId(reqVO.getReportUserId());
        }
        if (reqVO.getRepairUserId() != null) {
            existing.setRepairUserId(reqVO.getRepairUserId());
        }
        if (reqVO.getRepairStartTime() != null) {
            existing.setRepairStartTime(reqVO.getRepairStartTime());
        }
        if (reqVO.getRepairEndTime() != null) {
            existing.setRepairEndTime(reqVO.getRepairEndTime());
        }
        if (StringUtils.hasText(reqVO.getRepairResult())) {
            existing.setRepairResult(reqVO.getRepairResult());
        }
        existing.setRepairStatus(nextRepairStatus);
        existing.setRemark(reqVO.getRemark());
        fillTimeByRepairStatus(existing, previousRepairStatus);

        try {
            repairOrderRepository.saveAndFlush(existing);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateRepairNoException(e)) {
                throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_REPAIR_ORDER_NO_DUPLICATE);
            }
            throw e;
        }
        logger.info("[修改设备报修任务] id: {}, repairNo: {}", id, existing.getRepairNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEquipmentRepairOrder(Long id) {
        EquipmentRepairOrderEntity repairOrder = validateRepairOrderExists(id);
        if (REPAIRING_STATUS.equals(repairOrder.getRepairStatus())) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_REPAIR_STATUS_OPERATION_NOT_ALLOWED);
        }

        repairOrder.setRepairNo(buildDeletedRepairNo(repairOrder.getRepairNo(), repairOrder.getId()));
        repairOrder.setDeleted(true);
        repairOrderRepository.save(repairOrder);

        logger.info("[删除设备报修任务] id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public EquipmentRepairOrderRespVO getEquipmentRepairOrder(Long id) {
        EquipmentRepairOrderEntity repairOrder = validateRepairOrderExists(id);
        return EquipmentRepairOrderConvert.toRespVO(repairOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<EquipmentRepairOrderRespVO> getEquipmentRepairOrderPage(EquipmentRepairOrderPageReqVO reqVO) {
        Specification<EquipmentRepairOrderEntity> specification = EquipmentRepairOrderSpecifications.page(reqVO);

        long total = repairOrderRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        int pageSize = reqVO.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(reqVO.getPageNo(), totalPages);

        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "reportTime"));
        Page<EquipmentRepairOrderEntity> page = repairOrderRepository.findAll(specification, pageRequest);
        List<EquipmentRepairOrderEntity> list = page.getContent();

        return PageResult.of(EquipmentRepairOrderConvert.toRespVOList(list), total, pageNo, pageSize);
    }

    /**
     * 校验报修任务存在且未删除。
     *
     * @param id 报修任务主键
     * @return 报修任务实体
     */
    private EquipmentRepairOrderEntity validateRepairOrderExists(Long id) {
        return repairOrderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_REPAIR_ORDER_NOT_EXISTS));
    }

    /**
     * 校验报修单号唯一性。
     *
     * @param repairNo  报修单号
     * @param excludeId 排除的报修任务 id，创建时传 null
     */
    private void validateRepairNo(String repairNo, Long excludeId) {
        boolean exists = excludeId == null
                ? repairOrderRepository.existsByRepairNoAndDeletedFalse(repairNo)
                : repairOrderRepository.existsByRepairNoAndIdNotAndDeletedFalse(repairNo, excludeId);
        if (exists) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_REPAIR_ORDER_NO_DUPLICATE);
        }
    }

    /**
     * 校验设备存在且未删除，并对设备记录加写锁。
     *
     * @param equipmentId 设备台账 id
     * @return 设备台账实体
     */
    private EquipmentLedgerEntity validateEquipmentAvailableForUpdate(Long equipmentId) {
        EquipmentLedgerEntity equipmentLedger = ledgerRepository.findByIdAndDeletedFalseForUpdate(equipmentId)
                .orElseThrow(() -> new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_LEDGER_NOT_EXISTS));
        if ("SCRAPPED".equals(equipmentLedger.getEquipmentStatus())) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_STATUS_OPERATION_NOT_ALLOWED);
        }
        return equipmentLedger;
    }

    /**
     * 校验故障原理存在、未删除且适用于当前设备类别。
     *
     * @param faultPrincipleId 故障原理 id，可空
     * @param equipmentLedger  设备台账实体
     */
    private void validateFaultPrincipleAvailableForEquipment(Long faultPrincipleId, EquipmentLedgerEntity equipmentLedger) {
        if (faultPrincipleId == null) {
            return;
        }

        EquipmentFaultPrincipleEntity faultPrinciple = faultPrincipleRepository.findByIdAndDeletedFalseForUpdate(faultPrincipleId)
                .orElseThrow(() -> new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_FAULT_PRINCIPLE_NOT_EXISTS));
        Long applicableCategoryId = faultPrinciple.getCategoryId();
        if (applicableCategoryId != null && !applicableCategoryId.equals(equipmentLedger.getCategoryId())) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_FAULT_PRINCIPLE_CATEGORY_NOT_MATCH);
        }
    }

    /**
     * 校验报修状态流转是否合法。
     *
     * @param previousRepairStatus 当前状态
     * @param nextRepairStatus     目标状态
     */
    private void validateRepairStatusTransition(String previousRepairStatus, String nextRepairStatus) {
        if (previousRepairStatus == null || previousRepairStatus.equals(nextRepairStatus)) {
            return;
        }
        if (FINISHED_STATUS.equals(previousRepairStatus) || CANCELLED_STATUS.equals(previousRepairStatus)) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_REPAIR_STATUS_OPERATION_NOT_ALLOWED);
        }

        Set<String> allowedNextStatuses = switch (previousRepairStatus) {
            case "REPORTED" -> Set.of("ASSIGNED", REPAIRING_STATUS, FINISHED_STATUS, CANCELLED_STATUS);
            case "ASSIGNED" -> Set.of(REPAIRING_STATUS, FINISHED_STATUS, CANCELLED_STATUS);
            case "REPAIRING" -> Set.of(FINISHED_STATUS, CANCELLED_STATUS);
            default -> Set.of();
        };
        if (!allowedNextStatuses.contains(nextRepairStatus)) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_REPAIR_STATUS_OPERATION_NOT_ALLOWED);
        }
    }

    /**
     * 根据报修状态补充关键时间字段。
     *
     * @param repairOrder 报修任务实体
     */
    private void fillTimeByRepairStatus(EquipmentRepairOrderEntity repairOrder) {
        fillTimeByRepairStatus(repairOrder, null);
    }

    /**
     * 根据报修状态变化补充关键时间字段。
     *
     * @param repairOrder          报修任务实体
     * @param previousRepairStatus 当前状态，创建时传 null
     */
    private void fillTimeByRepairStatus(EquipmentRepairOrderEntity repairOrder, String previousRepairStatus) {
        LocalDateTime now = LocalDateTime.now();
        boolean statusChanged = previousRepairStatus == null || !previousRepairStatus.equals(repairOrder.getRepairStatus());
        if (statusChanged && REPAIRING_STATUS.equals(repairOrder.getRepairStatus()) && repairOrder.getRepairStartTime() == null) {
            repairOrder.setRepairStartTime(now);
        }
        if (statusChanged && FINISHED_STATUS.equals(repairOrder.getRepairStatus())) {
            if (repairOrder.getRepairStartTime() == null) {
                repairOrder.setRepairStartTime(now);
            }
            if (repairOrder.getRepairEndTime() == null) {
                repairOrder.setRepairEndTime(now);
            }
        }
        if (statusChanged && CANCELLED_STATUS.equals(repairOrder.getRepairStatus())) {
            repairOrder.setRepairEndTime(null);
        }
    }

    /**
     * 判断是否为报修单号唯一约束冲突。
     *
     * @param exception 数据库约束异常
     * @return true 是报修单号重复，false 不是
     */
    private boolean isDuplicateRepairNoException(DataIntegrityViolationException exception) {
        return exception.getMessage() != null && exception.getMessage().contains("uk_repair_no");
    }

    /**
     * 生成报修单号。
     *
     * @return 报修单号
     */
    private String buildRepairNo() {
        String timestampPart = LocalDateTime.now().format(REPAIR_NO_DATE_FORMATTER);
        String uniquePart = Long.toString(System.nanoTime(), 36).toUpperCase();
        String generatedRepairNo = REPAIR_NO_PREFIX + "-" + timestampPart + "-" + uniquePart;
        if (generatedRepairNo.length() <= REPAIR_NO_MAX_LENGTH) {
            return generatedRepairNo;
        }
        return generatedRepairNo.substring(0, REPAIR_NO_MAX_LENGTH);
    }

    /**
     * 构造逻辑删除后的报修单号。
     *
     * @param originalRepairNo 原报修单号
     * @param repairOrderId    报修任务主键
     * @return 删除态报修单号
     */
    private String buildDeletedRepairNo(String originalRepairNo, Long repairOrderId) {
        String suffix = DELETED_REPAIR_NO_SUFFIX_PREFIX + Long.toString(repairOrderId, 36).toUpperCase();
        int preservedLength = REPAIR_NO_MAX_LENGTH - suffix.length();
        if (preservedLength <= 0) {
            return suffix.substring(0, REPAIR_NO_MAX_LENGTH);
        }
        String prefix = originalRepairNo.length() <= preservedLength
                ? originalRepairNo
                : originalRepairNo.substring(0, preservedLength);
        return prefix + suffix;
    }
}
