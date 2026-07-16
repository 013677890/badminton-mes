package com.badminton.mes.module.quality.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordCreateReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordPageReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordSubmitReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionResultsSaveReqVO;

/**
 * 统一质量检验单应用服务契约。
 *
 * <p>检验单以生效方案为模板创建，创建时固化方案编号、版本和各方案项规则，确保后续主数据或方案调整
 * 不改变历史检验依据。检验单仅在 DRAFT 状态可录入结果；提交后进入 SUBMITTED，不再允许编辑。</p>
 */
public interface QualityInspectionRecordService {

    /**
     * 按检验类型和方案创建草稿检验单。
     *
     * <p>前置条件：方案已生效、到达生效日期、检验类型一致、包含方案项，且产品/客户适用范围匹配。
     * 首件、末件和巡检必须来源于工单；其他检验必须提供来源单据和产品。</p>
     * <p>副作用：创建 PENDING 放行状态的草稿检验单，并按方案项生成完整结果快照。</p>
     *
     * @param inspectionType 检验类型
     * @param request 方案及来源单据信息
     * @return 新检验单主键
     * @throws com.badminton.mes.common.exception.ServiceException 方案不可用、来源无效或适用范围不匹配时抛出
     */
    Long createRecord(String inspectionType, QualityInspectionRecordCreateReqVO request);

    /**
     * 保存草稿检验结果。
     *
     * <p>前置条件：检验单存在且处于 DRAFT；请求中的结果主键不得重复且必须属于该检验单。
     * 已给出判定时必须同时给出实测值，FAIL 结果必须填写缺陷描述。</p>
     * <p>副作用：更新请求覆盖到的结果记录，并在事务提交后清理检验单详情缓存；允许分批保存，
     * 完整性在最终提交时统一检查。</p>
     *
     * @param id 检验单主键
     * @param request 待保存的结果集合
     * @throws com.badminton.mes.common.exception.ServiceException 状态、结果归属或填写完整性不合法时抛出
     */
    void saveResults(Long id, QualityInspectionResultsSaveReqVO request);

    /**
     * 提交草稿检验单并形成最终结论与放行状态。
     *
     * <p>前置条件：全部必检项均有实测值和判定，任一 FAIL 均有缺陷描述；存在 FAIL 时不能判 PASS，
     * 不存在 FAIL 时只能判 PASS；非 PASS 结论必须填写处置意见。</p>
     * <p>副作用：状态迁移到 SUBMITTED，记录检验人和检验时间。PASS 或 CONCESSION 映射为 RELEASED，
     * 其他结论映射为 BLOCKED，并在提交后清理详情缓存。</p>
     *
     * @param id 检验单主键
     * @param request 最终结论及不合格处置信息
     * @throws com.badminton.mes.common.exception.ServiceException 结果不完整、结论矛盾或状态不允许时抛出
     */
    void submitRecord(Long id, QualityInspectionRecordSubmitReqVO request);

    /**
     * 查询检验单及其有序结果快照。
     *
     * @param id 检验单主键
     * @return 检验单详情
     * @throws com.badminton.mes.common.exception.ServiceException 检验单不存在时抛出
     */
    QualityInspectionRecordRespVO getRecord(Long id);

    /**
     * 分页查询检验单摘要，不加载结果明细。
     *
     * @param request 分页及筛选条件
     * @return 检验单摘要分页结果
     */
    PageResult<QualityInspectionRecordRespVO> getRecordPage(QualityInspectionRecordPageReqVO request);
}
