package com.badminton.mes.module.scene.service.impl;

import java.util.List;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.integration.dal.entity.DeviceCountRecordEntity;
import com.badminton.mes.module.integration.dal.entity.EquipmentBindingEntity;
import com.badminton.mes.module.production.dal.entity.DispatchOrderEntity;
import com.badminton.mes.module.scene.constants.SceneErrorCodeConstants;
import com.badminton.mes.module.scene.controller.vo.SceneWorkReportReverseReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneWorkReportSubmitReqVO;
import com.badminton.mes.module.scene.dal.entity.SceneDispatchDetailEntity;
import com.badminton.mes.module.scene.dal.entity.SceneProductionTaskEntity;
import com.badminton.mes.module.scene.dal.repository.SceneDispatchDetailRepository;
import com.badminton.mes.module.scene.dal.repository.SceneProductionTaskRepository;
import com.badminton.mes.module.scene.dal.repository.SceneWorkReportRepository;
import com.badminton.mes.module.scene.service.SceneWorkReportService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 报工入口服务，负责请求号幂等查询和并发唯一键冲突恢复。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@Service
public class SceneWorkReportServiceImpl implements SceneWorkReportService {

    private final SceneWorkReportRepository reportRepository;
    private final SceneWorkReportTransactionalService transactionalService;
    private final SceneProductionTaskRepository taskRepository;
    private final SceneDispatchDetailRepository detailRepository;

    public SceneWorkReportServiceImpl(SceneWorkReportRepository reportRepository,
                                      SceneWorkReportTransactionalService transactionalService,
                                      SceneProductionTaskRepository taskRepository,
                                      SceneDispatchDetailRepository detailRepository) {
        this.reportRepository = reportRepository;
        this.transactionalService = transactionalService;
        this.taskRepository = taskRepository;
        this.detailRepository = detailRepository;
    }

    @Override
    public Long submit(SceneWorkReportSubmitReqVO reqVO, Integer sourceType) {
        return reportRepository.findByRequestNoAndDeletedFalse(reqVO.getRequestNo())
                .map(report -> report.getId())
                .orElseGet(() -> submitNew(reqVO, sourceType));
    }

    @Override
    public Long reverse(Long id, SceneWorkReportReverseReqVO reqVO) {
        return reportRepository.findByRequestNoAndDeletedFalse(reqVO.getRequestNo())
                .map(report -> report.getId())
                .orElseGet(() -> reverseNew(id, reqVO));
    }

    @Override
    public Long createDeviceReport(DeviceCountRecordEntity sourceRecord,
                                   EquipmentBindingEntity binding,
                                   DispatchOrderEntity dispatchOrder) {
        SceneProductionTaskEntity task = taskRepository
                .findByDispatchOrderIdAndDeletedFalse(dispatchOrder.getId())
                .orElseThrow(() -> new ServiceException(SceneErrorCodeConstants.TASK_NOT_EXISTS));
        List<SceneDispatchDetailEntity> details = detailRepository
                .findByTaskIdAndDeletedFalseOrderBySeqAsc(task.getId());
        SceneDispatchDetailEntity detail = details.stream()
                .filter(item -> sourceRecord.getProcessId().equals(item.getProcessId()))
                .findFirst()
                .orElseThrow(() -> new ServiceException(SceneErrorCodeConstants.OPERATION_NOT_EXISTS));
        int quantity = Math.toIntExact(sourceRecord.getIncrementValue());
        SceneWorkReportSubmitReqVO request = new SceneWorkReportSubmitReqVO();
        request.setRequestNo("DEVICE_COUNT:" + sourceRecord.getId());
        request.setDispatchDetailId(detail.getId());
        request.setInputQuantity(quantity);
        request.setGoodQuantity(quantity);
        request.setDefectQuantity(0);
        request.setReworkQuantity(0);
        request.setReportTime(sourceRecord.getCollectTime());
        return submit(request, 2);
    }

    private Long submitNew(SceneWorkReportSubmitReqVO reqVO, Integer sourceType) {
        try {
            return transactionalService.submit(reqVO, sourceType);
        } catch (DataIntegrityViolationException exception) {
            return recoverRequest(reqVO.getRequestNo(), exception);
        }
    }

    private Long reverseNew(Long id, SceneWorkReportReverseReqVO reqVO) {
        try {
            return transactionalService.reverse(id, reqVO);
        } catch (DataIntegrityViolationException exception) {
            return recoverRequest(reqVO.getRequestNo(), exception);
        }
    }

    private Long recoverRequest(String requestNo, DataIntegrityViolationException exception) {
        return reportRepository.findByRequestNoAndDeletedFalse(requestNo)
                .map(report -> report.getId())
                .orElseThrow(() -> exception);
    }
}
