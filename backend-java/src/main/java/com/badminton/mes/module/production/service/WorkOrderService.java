package com.badminton.mes.module.production.service;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.production.controller.vo.WorkOrderMaterialRespVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderPageReqVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderProgressRespVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderRespVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderSaveReqVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderStatusLogRespVO;

/**
 * 生产工单 Service 接口，承载工单全部业务规则：
 * 档案校验、单号生成、状态机流转与详情缓存。
 *
 * <p>业务规则不通过时统一抛 {@code ServiceException}(EXC-013 应用内部推荐异常表达)，
 * 错误码见 {@code ProductionErrorCodeConstants}。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
public interface WorkOrderService {

    /**
     * 创建生产工单。
     *
     * <p>校验产品与车间档案有效后落库；产品名称、规格、单位按产品档案回填；
     * 工单号未传时按"WO+日期+流水"生成，流水由 Redis 原子自增保证并发唯一。
     *
     * @param reqVO 创建请求，字段级校验已由 Controller 完成
     * @return 新工单主键 id
     * @throws com.badminton.mes.common.exception.ServiceException 计划时间倒挂、
     *         产品/车间不可用或工单号重复时抛出
     */
    Long createWorkOrder(WorkOrderSaveReqVO reqVO);

    /**
     * 修改生产工单计划信息。
     *
     * <p>"已创建"状态允许修改全部计划字段；"已下达"状态仅允许修改计划数量与
     * 计划时间，且必须填写变更原因并记入状态日志(需求规则：下达后修改计划
     * 数量或交期应记录变更原因)。已下达修改计划数量后按 BOM 重算工单物料需求，
     * 新需求低于已领数量时阻止变更。修改成功后于事务提交后删除详情缓存；
     * 工单号不允许修改。
     *
     * @param id    工单主键
     * @param reqVO 修改请求
     * @throws com.badminton.mes.common.exception.ServiceException 工单不存在、
     *         状态不允许修改、档案校验不通过、已下达修改缺少变更原因
     *         或新物料需求低于已领数量时抛出
     */
    void updateWorkOrder(Long id, WorkOrderSaveReqVO reqVO);

    /**
     * 删除生产工单(逻辑删除)，仅"已创建"状态允许删除。
     *
     * <p>删除成功后清理详情缓存。
     *
     * @param id 工单主键
     * @throws com.badminton.mes.common.exception.ServiceException 工单不存在
     *         或状态不允许删除时抛出
     */
    void deleteWorkOrder(Long id);

    /**
     * 下达生产工单：已创建 → 已下达。
     *
     * <p>通过 CAS 更新完成状态流转，未维护 BOM 或工艺路线的工单不允许下达；
     * CAS 未命中时查明具体原因给出精确错误。下达成功后按 BOM 明细在同一事务内
     * 生成工单物料需求(需求数量 = 计划数 × 标准用量 ×(1 + 损耗率))、
     * 写状态日志并清理详情缓存。
     *
     * @param id 工单主键
     * @throws com.badminton.mes.common.exception.ServiceException 工单不存在、
     *         状态不允许下达、未维护 BOM/工艺路线、BOM 未生效或无明细时抛出
     */
    void releaseWorkOrder(Long id);

    /**
     * 暂停生产工单：已下达/生产中 → 暂停，原因必填并记入状态日志。
     *
     * @param id     工单主键
     * @param reason 暂停原因
     * @throws com.badminton.mes.common.exception.ServiceException 工单不存在
     *         或状态不允许暂停时抛出
     */
    void pauseWorkOrder(Long id, String reason);

    /**
     * 恢复生产工单：暂停 → 暂停前状态(从最近一条暂停日志还原，无日志时回到已下达)。
     *
     * @param id 工单主键
     * @throws com.badminton.mes.common.exception.ServiceException 工单不存在
     *         或状态不允许恢复时抛出
     */
    void resumeWorkOrder(Long id);

    /**
     * 完工生产工单：已下达/生产中 → 已完工。
     *
     * <p>完工数量不得超过 计划数量 ×(1 + 超产比例) 的上限，该校验与状态流转
     * 在同一条 UPDATE 内原子完成，避免并发报工绕过上限。
     * 任务级完成校验待现场管理模块建设后补充。
     *
     * @param id 工单主键
     * @throws com.badminton.mes.common.exception.ServiceException 工单不存在、
     *         状态不允许完工或完工数量超上限时抛出
     */
    void finishWorkOrder(Long id);

    /**
     * 关闭生产工单：已完工 → 已关闭。已关闭工单不能再生成生产任务单。
     *
     * @param id 工单主键
     * @throws com.badminton.mes.common.exception.ServiceException 工单不存在
     *         或状态不允许关闭时抛出
     */
    void closeWorkOrder(Long id);

    /**
     * 作废生产工单：已创建/已下达 → 已作废，原因必填并记入状态日志。
     * 作废成功后逻辑删除工单物料需求，避免齐套/领料聚合计入已作废工单。
     *
     * @param id     工单主键
     * @param reason 作废原因
     * @throws com.badminton.mes.common.exception.ServiceException 工单不存在
     *         或状态不允许作废时抛出
     */
    void cancelWorkOrder(Long id, String reason);

    /**
     * 查询工单物料需求明细，物料编码/名称按物料档案回填。
     *
     * @param id 工单主键
     * @return 物料需求列表，未下达或无 BOM 明细时为空集合(API-002)
     * @throws com.badminton.mes.common.exception.ServiceException 工单不存在时抛出
     */
    List<WorkOrderMaterialRespVO> getWorkOrderMaterials(Long id);

    /**
     * 查询工单状态日志，最新在前。
     *
     * @param id 工单主键
     * @return 状态日志列表，无数据时为空集合(API-002)
     * @throws com.badminton.mes.common.exception.ServiceException 工单不存在时抛出
     */
    List<WorkOrderStatusLogRespVO> getWorkOrderStatusLogs(Long id);

    /**
     * 查询工单详情，走 Cache Aside 缓存：优先读 Redis，未命中回源数据库并回写缓存。
     *
     * @param id 工单主键
     * @return 工单详情
     * @throws com.badminton.mes.common.exception.ServiceException 工单不存在时抛出
     */
    WorkOrderRespVO getWorkOrder(Long id);

    /**
     * 按主键集合批量查询工单进度。
     *
     * @param ids 工单主键集合
     * @return 进度列表，不存在的 id 不返回
     */
    List<WorkOrderProgressRespVO> getWorkOrderProgress(List<Long> ids);

    /**
     * 分页查询工单列表。
     *
     * <p>先 count 后查列表，总数为 0 时直接返回空页；
     * 请求页码超过总页数时按最后一页返回(API-009)。
     *
     * @param reqVO 分页筛选条件
     * @return 分页结果，无数据时 list 为空集合而非 null(API-002)
     */
    PageResult<WorkOrderRespVO> getWorkOrderPage(WorkOrderPageReqVO reqVO);
}
