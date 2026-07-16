package com.badminton.mes.module.quality.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanPageReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanSaveReqVO;

/**
 * 质量检验标准方案 Service。
 *
 * <p>由 {@code QualityInspectionPlanController} 调用，负责方案版本、审核和启停状态；
 * 检验单 Service 在生成检验记录时读取已生效方案，故实现类必须维护版本和状态约束。
 *
 * @author MES 开发组
 * @date 2026/07/16
 */
public interface QualityInspectionPlanService {

    /** 创建检验标准方案草稿。 */
    Long createPlan(QualityInspectionPlanSaveReqVO request);

    /** 修改尚未被业务锁定的检验方案。 */
    void updatePlan(Long id, QualityInspectionPlanSaveReqVO request);

    /** 逻辑删除检验方案草稿或历史版本。 */
    void deletePlan(Long id);

    /** 审核方案，使其具备被检验单引用的资格。 */
    void auditPlan(Long id);

    /** 停用已生效方案，阻止后续新检验单继续引用。 */
    void disablePlan(Long id);

    /** 以当前方案复制创建一个新的版本草稿，并返回新版本主键。 */
    Long createNewVersion(Long id);

    /** 查询检验方案详情及其检验项目。 */
    QualityInspectionPlanRespVO getPlan(Long id);

    /** 按产品、版本和状态分页查询检验方案。 */
    PageResult<QualityInspectionPlanRespVO> getPlanPage(QualityInspectionPlanPageReqVO request);
}
