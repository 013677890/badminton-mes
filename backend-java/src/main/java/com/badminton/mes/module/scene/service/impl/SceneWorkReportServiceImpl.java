package com.badminton.mes.module.scene.service.impl;

import com.badminton.mes.module.scene.controller.vo.SceneWorkReportReverseReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneWorkReportSubmitReqVO;
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

    public SceneWorkReportServiceImpl(SceneWorkReportRepository reportRepository,
                                      SceneWorkReportTransactionalService transactionalService) {
        this.reportRepository = reportRepository;
        this.transactionalService = transactionalService;
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
