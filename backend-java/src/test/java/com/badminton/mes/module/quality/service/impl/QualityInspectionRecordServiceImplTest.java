package com.badminton.mes.module.quality.service.impl;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.production.service.WorkOrderService;
import com.badminton.mes.module.quality.constants.QualityErrorCodeConstants;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordSubmitReqVO;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionRecordEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionResultEntity;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionItemRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionPlanItemRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionPlanRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionRecordRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionResultRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 质量不良数量与 B 组报表契约测试。
 *
 * @author 田相利
 * @date 2026/07/14
 */
class QualityInspectionRecordServiceImplTest {

    private final QualityInspectionRecordRepository recordRepository =
            mock(QualityInspectionRecordRepository.class);
    private final QualityInspectionResultRepository resultRepository =
            mock(QualityInspectionResultRepository.class);
    private final QualityInspectionRecordServiceImpl service = new QualityInspectionRecordServiceImpl(
            recordRepository, resultRepository, mock(QualityInspectionPlanRepository.class),
            mock(QualityInspectionPlanItemRepository.class), mock(QualityInspectionItemRepository.class),
            mock(WorkOrderService.class));

    @Test
    void failedInspectionPersistsDefectQuantityAndGroupNumber() {
        QualityInspectionRecordEntity record = draftRecord();
        when(recordRepository.findByIdAndDeletedFalseForUpdate(1L)).thenReturn(Optional.of(record));
        when(resultRepository.findByInspectionRecordIdOrderBySortOrderAscIdAsc(1L))
                .thenReturn(List.of(failedResult()));
        QualityInspectionRecordSubmitReqVO request = failedRequest(3);

        service.submitRecord(1L, request);

        assertThat(record.getRecordStatus()).isEqualTo("SUBMITTED");
        assertThat(record.getDefectQuantity()).isEqualTo(3);
        assertThat(record.getDefectGroupNo()).isEqualTo("DEFECT-001");
    }

    @Test
    void failedInspectionRejectsQuantityAboveSample() {
        QualityInspectionRecordEntity record = draftRecord();
        when(recordRepository.findByIdAndDeletedFalseForUpdate(1L)).thenReturn(Optional.of(record));
        when(resultRepository.findByInspectionRecordIdOrderBySortOrderAscIdAsc(1L))
                .thenReturn(List.of(failedResult()));

        assertThatThrownBy(() -> service.submitRecord(1L, failedRequest(11)))
                .isInstanceOfSatisfying(ServiceException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(QualityErrorCodeConstants.RECORD_RESULTS_INCOMPLETE));
    }

    private QualityInspectionRecordEntity draftRecord() {
        QualityInspectionRecordEntity record = new QualityInspectionRecordEntity();
        record.setId(1L);
        record.setRecordStatus("DRAFT");
        record.setSampleQuantity(10);
        return record;
    }

    private QualityInspectionResultEntity failedResult() {
        QualityInspectionResultEntity result = new QualityInspectionResultEntity();
        result.setMeasuredValue("NG");
        result.setJudgmentResult("FAIL");
        result.setDefectDescription("外观破损");
        return result;
    }

    private QualityInspectionRecordSubmitReqVO failedRequest(int defectQuantity) {
        QualityInspectionRecordSubmitReqVO request = new QualityInspectionRecordSubmitReqVO();
        request.setConclusion("REWORK");
        request.setNonconformanceDescription("外观破损");
        request.setDisposition("返修");
        request.setDefectQuantity(defectQuantity);
        request.setDefectGroupNo(" DEFECT-001 ");
        return request;
    }
}
