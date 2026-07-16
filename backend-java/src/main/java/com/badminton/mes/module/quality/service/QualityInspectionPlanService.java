package com.badminton.mes.module.quality.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanPageReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanSaveReqVO;

/**
 * 检验标准方案应用服务契约。
 *
 * <p>方案遵循“草稿 DRAFT - 审核生效 EFFECTIVE - 停用 DISABLED”的单向状态机。
 * 草稿可编辑或删除；审核后不可直接修改，只能从非草稿版本复制出新的草稿版本。</p>
 *
 * <p>方案项在保存时继承或覆盖检验项目规则，形成方案版本自己的规则快照，后续主数据调整
 * 不会自动改写已保存方案项。</p>
 */
public interface QualityInspectionPlanService {

    /**
     * 创建版本号为 1 的草稿方案。
     *
     * <p>前置条件：同方案编码的初始版本不存在；方案项不重复，引用的检验项目存在、启用且规则有效。</p>
     * <p>副作用：在同一事务中保存方案主表及全部方案项快照。</p>
     *
     * @param request 方案主数据与方案项
     * @return 新方案主键
     * @throws com.badminton.mes.common.exception.ServiceException 编码冲突或方案项非法时抛出
     */
    Long createPlan(QualityInspectionPlanSaveReqVO request);

    /**
     * 更新草稿方案及其全部方案项。
     *
     * <p>前置条件：方案存在且处于 DRAFT；方案编码不可变；全部方案项通过引用和规则校验。</p>
     * <p>副作用：悲观锁定方案，以“删除旧项后重建”的方式替换方案项快照，并在提交后清理方案缓存。</p>
     *
     * @param id 方案主键
     * @param request 完整方案定义
     * @throws com.badminton.mes.common.exception.ServiceException 方案不存在、非草稿、编码变化或方案项非法时抛出
     */
    void updatePlan(Long id, QualityInspectionPlanSaveReqVO request);

    /**
     * 逻辑删除草稿方案。
     *
     * <p>仅 DRAFT 可删除；方案项会被物理移除，方案编码改为删除占位值，默认标记同时取消。</p>
     *
     * @param id 方案主键
     * @throws com.badminton.mes.common.exception.ServiceException 方案不存在、非草稿或编码约束冲突时抛出
     */
    void deletePlan(Long id);

    /**
     * 审核草稿并使其生效。
     *
     * <p>前置条件：方案处于 DRAFT 且至少包含一个方案项。同产品、客户、检验类型适用范围内，
     * 不允许同时存在多个生效的默认方案。</p>
     * <p>副作用：通过悲观锁串行化默认方案竞争，状态迁移到 EFFECTIVE，补齐生效日期并记录审核人和时间。</p>
     *
     * @param id 方案主键
     * @throws com.badminton.mes.common.exception.ServiceException 状态、方案项或默认范围冲突时抛出
     */
    void auditPlan(Long id);

    /**
     * 停用生效方案。
     *
     * <p>仅 EFFECTIVE 可迁移到 DISABLED；停用同时取消默认方案标记，且该版本不可恢复为草稿。</p>
     *
     * @param id 方案主键
     * @throws com.badminton.mes.common.exception.ServiceException 方案不存在或并非生效状态时抛出
     */
    void disablePlan(Long id);

    /**
     * 从非草稿版本复制出一个新草稿版本。
     *
     * <p>前置条件：源版本存在、不是 DRAFT 且包含方案项。</p>
     * <p>副作用：悲观锁定同方案编码的全部版本，按最大版本号加一，并完整复制方案项规则快照；
     * 新版本不继承生效日期、默认标记和审核信息。</p>
     *
     * @param id 源方案版本主键
     * @return 新草稿版本主键
     * @throws com.badminton.mes.common.exception.ServiceException 源版本、状态或方案项不满足条件时抛出
     */
    Long createNewVersion(Long id);

    /**
     * 查询方案详情及有序方案项。
     *
     * <p>方案项中的判定规则来自版本快照；项目编码、名称、值类型和单位从当前检验项目主数据冗余装配。</p>
     *
     * @param id 方案主键
     * @return 方案详情
     * @throws com.badminton.mes.common.exception.ServiceException 方案不存在时抛出
     */
    QualityInspectionPlanRespVO getPlan(Long id);

    /**
     * 分页查询方案摘要，不加载方案项明细。
     *
     * @param request 分页及筛选条件
     * @return 方案摘要分页结果
     */
    PageResult<QualityInspectionPlanRespVO> getPlanPage(QualityInspectionPlanPageReqVO request);
}
