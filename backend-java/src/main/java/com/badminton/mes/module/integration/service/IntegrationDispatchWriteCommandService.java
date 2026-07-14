package com.badminton.mes.module.integration.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.integration.constants.IntegrationErrorCodeConstants;
import com.badminton.mes.module.integration.controller.vo.ExternalDispatchOrderWriteReqVO;
import com.badminton.mes.module.integration.dal.entity.IntegrationWriteLogEntity;
import com.badminton.mes.module.integration.dal.repository.IntegrationWriteLogRepository;
import com.badminton.mes.module.integration.enums.IntegrationInterfaceTypeEnum;
import com.badminton.mes.module.integration.enums.IntegrationWriteStatusEnum;
import com.badminton.mes.module.integration.service.dto.IntegrationCommandResult;
import com.badminton.mes.module.production.controller.vo.DispatchSaveReqVO;
import com.badminton.mes.module.production.dal.entity.DispatchOrderEntity;
import com.badminton.mes.module.production.dal.entity.ProductionLineEntity;
import com.badminton.mes.module.production.dal.entity.ShiftEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.repository.DispatchOrderRepository;
import com.badminton.mes.module.production.dal.repository.ProductionLineRepository;
import com.badminton.mes.module.production.dal.repository.ShiftRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.service.DispatchOrderService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 外部生产任务单写入命令服务。
 *
 * <p>负责解析外部编码、复用派工业务规则并记录接口结果，避免继续扩大通用写入命令服务。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Service
public class IntegrationDispatchWriteCommandService {

    private final WorkOrderRepository workOrderRepository;

    private final ProductionLineRepository productionLineRepository;

    private final ShiftRepository shiftRepository;

    private final DispatchOrderRepository dispatchOrderRepository;

    private final DispatchOrderService dispatchOrderService;

    private final IntegrationWriteLogRepository writeLogRepository;

    private final IntegrationAuditService auditService;

    /**
     * 构造外部生产任务单写入命令服务。
     *
     * @param workOrderRepository      生产工单 Repository
     * @param productionLineRepository 产线 Repository
     * @param shiftRepository          班次 Repository
     * @param dispatchOrderRepository  派工单 Repository
     * @param dispatchOrderService     派工单 Service
     * @param writeLogRepository       接口写入日志 Repository
     * @param auditService             接口审计服务
     */
    public IntegrationDispatchWriteCommandService(
            WorkOrderRepository workOrderRepository,
            ProductionLineRepository productionLineRepository,
            ShiftRepository shiftRepository,
            DispatchOrderRepository dispatchOrderRepository,
            DispatchOrderService dispatchOrderService,
            IntegrationWriteLogRepository writeLogRepository,
            IntegrationAuditService auditService) {
        this.workOrderRepository = workOrderRepository;
        this.productionLineRepository = productionLineRepository;
        this.shiftRepository = shiftRepository;
        this.dispatchOrderRepository = dispatchOrderRepository;
        this.dispatchOrderService = dispatchOrderService;
        this.writeLogRepository = writeLogRepository;
        this.auditService = auditService;
    }

    /**
     * 按来源系统和外部任务单号幂等写入生产任务单。
     *
     * <p>派工单表没有外部来源字段，因此复用接口日志作为幂等结果索引；实际创建仍走
     * {@link DispatchOrderService}，统一执行工单状态、剩余数量、工作日和产能校验。
     *
     * @param reqVO    外部任务单请求
     * @param snapshot 原始请求快照
     * @return 命令结果
     */
    @Transactional(rollbackFor = Exception.class)
    public IntegrationCommandResult writeDispatchOrder(
            ExternalDispatchOrderWriteReqVO reqVO,
            String snapshot) {
        String sourceSystem = normalizeCode(reqVO.getSourceSystem());
        String externalNo = reqVO.getExternalDispatchOrderNo().trim();
        Optional<IntegrationWriteLogEntity> existingLog = findExternalDispatchLog(
                sourceSystem, externalNo);
        if (existingLog.isPresent()) {
            return buildDuplicateResult(existingLog.get(), sourceSystem, externalNo, snapshot);
        }

        validatePlanTime(reqVO);
        WorkOrderEntity workOrder = requireWorkOrder(reqVO.getWorkOrderNo());
        ProductionLineEntity productionLine = requireProductionLine(reqVO.getLineCode());
        ShiftEntity shift = requireShift(reqVO.getShiftCode());
        Long dispatchId = dispatchOrderService.createDispatch(
                buildDispatchRequest(reqVO, workOrder, productionLine, shift));
        DispatchOrderEntity dispatchOrder = dispatchOrderRepository.findByIdAndDeletedFalse(dispatchId)
                .orElseThrow(() -> new ServiceException(IntegrationErrorCodeConstants.WRITE_CONFLICT));
        Long logId = auditService.recordResult(
                IntegrationInterfaceTypeEnum.DISPATCH_ORDER_WRITE,
                sourceSystem,
                externalNo,
                snapshot,
                IntegrationWriteStatusEnum.SUCCESS,
                dispatchOrder.getId(),
                dispatchOrder.getDispatchNo());
        return new IntegrationCommandResult(
                dispatchOrder.getId(), dispatchOrder.getDispatchNo(), false, logId);
    }

    /**
     * 查询已成功处理的外部任务单日志。
     *
     * @param sourceSystem 来源系统
     * @param externalNo   外部任务单号
     * @return 最近一条成功或重复日志
     */
    @Transactional(readOnly = true)
    public Optional<IntegrationWriteLogEntity> findExternalDispatchLog(
            String sourceSystem,
            String externalNo) {
        return writeLogRepository
                .findFirstByInterfaceTypeAndSourceSystemAndBusinessKeyAndWriteStatusInOrderByIdDesc(
                        IntegrationInterfaceTypeEnum.DISPATCH_ORDER_WRITE.getValue(),
                        sourceSystem,
                        externalNo,
                        List.of(IntegrationWriteStatusEnum.SUCCESS.getStatus(),
                                IntegrationWriteStatusEnum.DUPLICATE.getStatus()));
    }

    /**
     * 为重复请求记录新的审计日志并返回既有业务结果。
     *
     * @param existingLog  既有成功或重复日志
     * @param sourceSystem 来源系统
     * @param externalNo   外部任务单号
     * @param snapshot     当前请求快照
     * @return 重复处理结果
     */
    private IntegrationCommandResult buildDuplicateResult(
            IntegrationWriteLogEntity existingLog,
            String sourceSystem,
            String externalNo,
            String snapshot) {
        Long logId = auditService.recordResult(
                IntegrationInterfaceTypeEnum.DISPATCH_ORDER_WRITE,
                sourceSystem,
                externalNo,
                snapshot,
                IntegrationWriteStatusEnum.DUPLICATE,
                existingLog.getResultId(),
                existingLog.getResultNo());
        return new IntegrationCommandResult(
                existingLog.getResultId(), existingLog.getResultNo(), true, logId);
    }

    /**
     * 校验计划开始和结束时间顺序。
     *
     * @param reqVO 外部任务单请求
     */
    private void validatePlanTime(ExternalDispatchOrderWriteReqVO reqVO) {
        if (reqVO.getPlanEndTime().isBefore(reqVO.getPlanStartTime())) {
            throw new ServiceException(
                    IntegrationErrorCodeConstants.EXTERNAL_DISPATCH_PLAN_TIME_INVALID);
        }
    }

    /**
     * 查询外部任务单关联的生产工单。
     *
     * @param workOrderNo 生产工单号
     * @return 生产工单实体
     */
    private WorkOrderEntity requireWorkOrder(String workOrderNo) {
        return workOrderRepository.findByWorkOrderNoAndDeletedFalse(workOrderNo.trim())
                .orElseThrow(() -> new ServiceException(
                        IntegrationErrorCodeConstants.EXTERNAL_DISPATCH_WORK_ORDER_NOT_AVAILABLE));
    }

    /**
     * 查询启用产线。
     *
     * @param lineCode 产线编码
     * @return 产线实体
     */
    private ProductionLineEntity requireProductionLine(String lineCode) {
        ProductionLineEntity productionLine = productionLineRepository
                .findByLineCodeAndDeletedFalse(normalizeCode(lineCode))
                .orElseThrow(() -> new ServiceException(
                        IntegrationErrorCodeConstants.EXTERNAL_DISPATCH_LINE_NOT_AVAILABLE));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(productionLine.getStatus())) {
            throw new ServiceException(
                    IntegrationErrorCodeConstants.EXTERNAL_DISPATCH_LINE_NOT_AVAILABLE);
        }
        return productionLine;
    }

    /**
     * 查询启用班次。
     *
     * @param shiftCode 班次编码
     * @return 班次实体
     */
    private ShiftEntity requireShift(String shiftCode) {
        ShiftEntity shift = shiftRepository.findByShiftCodeAndDeletedFalse(normalizeCode(shiftCode))
                .orElseThrow(() -> new ServiceException(
                        IntegrationErrorCodeConstants.EXTERNAL_DISPATCH_SHIFT_NOT_AVAILABLE));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(shift.getStatus())) {
            throw new ServiceException(
                    IntegrationErrorCodeConstants.EXTERNAL_DISPATCH_SHIFT_NOT_AVAILABLE);
        }
        return shift;
    }

    /**
     * 构造派工单创建请求。
     *
     * @param reqVO          外部任务单请求
     * @param workOrder      生产工单
     * @param productionLine 产线
     * @param shift          班次
     * @return 派工单创建请求
     */
    private DispatchSaveReqVO buildDispatchRequest(
            ExternalDispatchOrderWriteReqVO reqVO,
            WorkOrderEntity workOrder,
            ProductionLineEntity productionLine,
            ShiftEntity shift) {
        DispatchSaveReqVO dispatchRequest = new DispatchSaveReqVO();
        dispatchRequest.setWorkOrderId(workOrder.getId());
        dispatchRequest.setLineId(productionLine.getId());
        dispatchRequest.setShiftId(shift.getId());
        dispatchRequest.setPlanDate(reqVO.getPlanDate());
        dispatchRequest.setPlanQuantity(reqVO.getPlanQuantity());
        dispatchRequest.setPlanStartTime(reqVO.getPlanStartTime());
        dispatchRequest.setPlanEndTime(reqVO.getPlanEndTime());
        dispatchRequest.setSuggest(Boolean.FALSE);
        return dispatchRequest;
    }

    /**
     * 规范化外部编码。
     *
     * @param value 原始编码
     * @return 去空格并转大写后的编码
     */
    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
