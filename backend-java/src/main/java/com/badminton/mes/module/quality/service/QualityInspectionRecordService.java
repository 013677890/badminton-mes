package com.badminton.mes.module.quality.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordCreateReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordPageReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordSubmitReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionResultsSaveReqVO;

/** 统一质量检验单 Service。 */
public interface QualityInspectionRecordService {

    Long createRecord(String inspectionType, QualityInspectionRecordCreateReqVO request);

    void saveResults(Long id, QualityInspectionResultsSaveReqVO request);

    void submitRecord(Long id, QualityInspectionRecordSubmitReqVO request);

    QualityInspectionRecordRespVO getRecord(Long id);

    PageResult<QualityInspectionRecordRespVO> getRecordPage(QualityInspectionRecordPageReqVO request);
}
