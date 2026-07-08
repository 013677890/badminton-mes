package com.badminton.mes.module.production.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.production.controller.vo.WorkOrderPageReqVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderRespVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderSaveReqVO;

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
     * 修改生产工单计划信息，仅"已创建"状态允许修改。
     *
     * <p>修改成功后删除详情缓存；工单号不允许修改，请求中的工单号被忽略。
     *
     * @param id    工单主键
     * @param reqVO 修改请求
     * @throws com.badminton.mes.common.exception.ServiceException 工单不存在、
     *         状态不允许修改或档案校验不通过时抛出
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
     * CAS 未命中时查明具体原因给出精确错误。下达成功后清理详情缓存。
     *
     * @param id 工单主键
     * @throws com.badminton.mes.common.exception.ServiceException 工单不存在、
     *         状态不允许下达或未维护 BOM/工艺路线时抛出
     */
    void releaseWorkOrder(Long id);

    /**
     * 查询工单详情，走 Cache Aside 缓存：优先读 Redis，未命中回源数据库并回写缓存。
     *
     * @param id 工单主键
     * @return 工单详情
     * @throws com.badminton.mes.common.exception.ServiceException 工单不存在时抛出
     */
    WorkOrderRespVO getWorkOrder(Long id);

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
