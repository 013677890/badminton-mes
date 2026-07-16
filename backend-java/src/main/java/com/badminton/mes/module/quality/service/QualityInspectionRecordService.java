package com.badminton.mes.module.quality.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordCreateReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordPageReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordSubmitReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionResultsSaveReqVO;

/**
 * 统一质量检验单 Service。
 *
 * <p>由质量检验 Controller 创建和提交检验单；安灯模块也会通过本接口读取已提交的
 * 检验结果来校验异常关联关系。结果保存与提交是两个阶段，提交后通常不再允许修改。
 *
 * @author MES 开发组
 * @date 2026/07/16
 */
public interface QualityInspectionRecordService {

    /** 按检验类型和请求内容创建检验单草稿。 */
    Long createRecord(String inspectionType, QualityInspectionRecordCreateReqVO request);

    /** 保存检验项目结果，但不改变检验单的最终提交状态。 */
    void saveResults(Long id, QualityInspectionResultsSaveReqVO request);

    /** 校验结果完整性并提交检验单，使其可被质量报表和安灯关联读取。 */
    void submitRecord(Long id, QualityInspectionRecordSubmitReqVO request);

    /** 查询检验单详情及结果明细。 */
    QualityInspectionRecordRespVO getRecord(Long id);

    /** 按检验类型、状态和时间范围分页查询检验单。 */
    PageResult<QualityInspectionRecordRespVO> getRecordPage(QualityInspectionRecordPageReqVO request);
}
