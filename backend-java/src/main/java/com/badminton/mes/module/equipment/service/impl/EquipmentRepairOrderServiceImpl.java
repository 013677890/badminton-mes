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
 * <p>围绕设备台账编排上报、派工、维修中、完成或取消的单向状态机。创建和更新会悲观锁定设备及
 * 可选故障原理，使设备可用性和类别适配校验在事务提交前保持稳定；报修单号采用应用层预查加
 * 数据库唯一索引兜底。逻辑删除改写单号以保留任务审计快照并释放原业务值。
 *
 * @author 角色C
 * @date 2026/07/10
 */
@Service
public class EquipmentRepairOrderServiceImpl implements EquipmentRepairOrderService {

    /** 记录报修任务关键写操作。 */
    private static final Logger logger = LoggerFactory.getLogger(EquipmentRepairOrderServiceImpl.class);

    /** TODO(角色C, 2026/07/10): 认证模块建设后，改为从登录上下文获取当前用户 id */
    private static final Long DEFAULT_OPERATOR_ID = 1L;

    /** 新建报修单默认从已上报状态进入状态机。 */
    private static final String DEFAULT_REPAIR_STATUS = "REPORTED";

    /** 维修中状态，进入时自动补齐开始时间。 */
    private static final String REPAIRING_STATUS = "REPAIRING";

    /** 已完成终态，进入时补齐缺失的开始和结束时间。 */
    private static final String FINISHED_STATUS = "FINISHED";

    /** 已取消终态，进入时清除结束时间。 */
    private static final String CANCELLED_STATUS = "CANCELLED";

    /** 报修单号数据库字段最大长度，自动和删除态单号均需遵守。 */
    private static final int REPAIR_NO_MAX_LENGTH = 32;

    /** 逻辑删除单号后缀前缀，用于释放原报修单号唯一约束。 */
    private static final String DELETED_REPAIR_NO_SUFFIX_PREFIX = "_D";

    /** 自动报修单号业务前缀。 */
    private static final String REPAIR_NO_PREFIX = "REP";

    /** 自动报修单号中的秒级日期格式。 */
    private static final DateTimeFormatter REPAIR_NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 报修仓储，提供有效任务查询和单号唯一约束落库。 */
    private final EquipmentRepairOrderRepository repairOrderRepository;

    /** 台账仓储，用于锁定并校验报修设备。 */
    private final EquipmentLedgerRepository ledgerRepository;

    /** 故障原理仓储，用于锁定并校验可选故障知识及适用类别。 */
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

    /**
     * 创建报修任务并补齐单号、上报信息、初始状态及对应时间。
     *
     * <p>设备与故障原理在事务内加写锁，避免校验后被并发删除或换类；自动单号仍需经过应用层查重
     * 和数据库唯一索引双重保护。
     *
     * @param reqVO 报修任务创建数据
     * @return 新报修任务主键
     */
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

    /**
     * 更新报修任务并执行单向状态迁移。
     *
     * <p>先校验目标设备与故障原理适配关系，再验证状态机；只有状态发生变化时才自动补齐关键时间，
     * 避免重复保存覆盖人工录入或既有审计时间。
     *
     * @param id 报修任务主键
     * @param reqVO 报修任务更新数据
     */
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

    /**
     * 逻辑删除非维修中的报修任务。
     *
     * <p>正在维修的任务必须先完成或取消，避免删除活跃流程；删除态单号在长度限制内附加主键后缀，
     * 释放原唯一键并保留历史任务。
     *
     * @param id 报修任务主键
     */
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

    /**
     * 查询一条未删除报修任务并生成响应快照。
     *
     * @param id 报修任务主键
     * @return 报修任务详情快照
     */
    @Override
    @Transactional(readOnly = true)
    public EquipmentRepairOrderRespVO getEquipmentRepairOrder(Long id) {
        EquipmentRepairOrderEntity repairOrder = validateRepairOrderExists(id);
        return EquipmentRepairOrderConvert.toRespVO(repairOrder);
    }

    /**
     * 分页查询报修任务；空结果跳过列表查询，越界页码收敛到最后一页。
     *
     * @param reqVO 分页及筛选条件
     * @return 按上报时间倒序排列的分页快照
     */
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
     * 校验有效报修单号唯一性，更新场景排除当前任务。
     *
     * <p>应用层预查负责快速失败，并发事务的最终冲突由数据库唯一索引兜底。
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
     * <p>锁定设备可避免报修事务执行期间设备被并发删除或报废。
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
     * <p>故障原理未限定类别时可用于任意设备；限定类别时必须与已锁定设备的类别完全一致。对故障
     * 原理加写锁可防止适用范围在当前事务提交前被并发修改。
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
     * <p>允许保持原状态；已完成和已取消为不可逆终态。其余状态仅可沿上报、派工、维修中方向前进，
     * 并可在允许节点结束或取消，禁止任何回退。
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
     * <p>首次进入维修中补开始时间；直接完成时同时补齐开始和结束时间；取消时清除结束时间，避免
     * 取消记录被统计为已完成维修。调用方提供的非空时间优先保留。
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
     * <p>只识别目标索引，其他数据库完整性异常继续上抛，避免掩盖真实约束问题。
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
     * <p>时间部分便于人工识别，纳秒计数片段降低同秒碰撞；超过字段长度时截断，最终唯一性仍由
     * 数据库索引保证。
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
     * <p>优先保留原单号前缀，并附加由主键生成的稳定后缀；必要时截断原值以遵守字段长度上限。
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
