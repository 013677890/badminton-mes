package com.badminton.mes.module.quality.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanPageReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanSaveReqVO;

/** 检验标准方案 Service。 */
public interface QualityInspectionPlanService {

    Long createPlan(QualityInspectionPlanSaveReqVO request);

    void updatePlan(Long id, QualityInspectionPlanSaveReqVO request);

    void deletePlan(Long id);

    void auditPlan(Long id);

    void disablePlan(Long id);

    Long createNewVersion(Long id);

    QualityInspectionPlanRespVO getPlan(Long id);

    PageResult<QualityInspectionPlanRespVO> getPlanPage(QualityInspectionPlanPageReqVO request);
}
