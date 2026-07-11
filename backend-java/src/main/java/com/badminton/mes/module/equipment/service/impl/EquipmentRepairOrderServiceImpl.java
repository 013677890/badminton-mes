package com.badminton.mes.module.equipment.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.equipment.constants.EquipmentErrorCodeConstants;
import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentRepairOrderSaveReqVO;
import com.badminton.mes.module.equipment.convert.EquipmentRepairOrderConvert;
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
        validateEquipmentAvailable(reqVO.getEquipmentId());
        validateFaultPrincipleAvailable(reqVO.getFaultPrincipleId());

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
            if (e.getMessage() != null && e.getMessage().contains("uk_repair_no")) {
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
        validateEquipmentAvailable(reqVO.getEquipmentId());
        validateFaultPrincipleAvailable(reqVO.getFaultPrincipleId());

        String repairNo = StringUtils.hasText(reqVO.getRepairNo()) ? reqVO.getRepairNo() : existing.getRepairNo();
        validateRepairNo(repairNo, id);

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
        existing.setRepairUserId(reqVO.getRepairUserId());
        existing.setRepairStartTime(reqVO.getRepairStartTime());
        existing.setRepairEndTime(reqVO.getRepairEndTime());
        existing.setRepairResult(reqVO.getRepairResult());
        if (reqVO.getRepairStatus() != null) {
            existing.setRepairStatus(reqVO.getRepairStatus());
        }
        existing.setRemark(reqVO.getRemark());
        fillTimeByRepairStatus(existing);

        repairOrderRepository.save(existing);
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
     * 校验设备存在且未删除。
     *
     * @param equipmentId 设备台账 id
     */
    private void validateEquipmentAvailable(Long equipmentId) {
        EquipmentLedgerEntity equipmentLedger = ledgerRepository.findByIdAndDeletedFalse(equipmentId)
                .orElseThrow(() -> new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_LEDGER_NOT_EXISTS));
        if ("SCRAPPED".equals(equipmentLedger.getEquipmentStatus())) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_STATUS_OPERATION_NOT_ALLOWED);
        }
    }

    /**
     * 校验故障原理存在且未删除。
     *
     * @param faultPrincipleId 故障原理 id，可空
     */
    private void validateFaultPrincipleAvailable(Long faultPrincipleId) {
        if (faultPrincipleId == null) {
            return;
        }
        boolean exists = faultPrincipleRepository.findByIdAndDeletedFalse(faultPrincipleId).isPresent();
        if (!exists) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_FAULT_PRINCIPLE_NOT_EXISTS);
        }
    }

    /**
     * 根据报修状态补充关键时间字段。
     *
     * @param repairOrder 报修任务实体
     */
    private void fillTimeByRepairStatus(EquipmentRepairOrderEntity repairOrder) {
        LocalDateTime now = LocalDateTime.now();
        if (REPAIRING_STATUS.equals(repairOrder.getRepairStatus()) && repairOrder.getRepairStartTime() == null) {
            repairOrder.setRepairStartTime(now);
        }
        if (FINISHED_STATUS.equals(repairOrder.getRepairStatus())) {
            if (repairOrder.getRepairStartTime() == null) {
                repairOrder.setRepairStartTime(now);
            }
            if (repairOrder.getRepairEndTime() == null) {
                repairOrder.setRepairEndTime(now);
            }
        }
        if (CANCELLED_STATUS.equals(repairOrder.getRepairStatus())) {
            repairOrder.setRepairEndTime(null);
        }
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
