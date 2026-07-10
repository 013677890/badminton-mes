package com.badminton.mes.module.production.service;

import java.time.LocalDate;
import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.production.controller.vo.DispatchAdjustLogRespVO;
import com.badminton.mes.module.production.controller.vo.DispatchPageReqVO;
import com.badminton.mes.module.production.controller.vo.DispatchRespVO;
import com.badminton.mes.module.production.controller.vo.DispatchSaveReqVO;
import com.badminton.mes.module.production.controller.vo.DispatchSuggestRespVO;

/**
 * 派工单 Service 接口。
 *
 * <p>业务规则见 wiki/16-齐套分析与派工单设计.md 与 wiki/01-生产订单需求分析.md §3：
 * 派工数量不超工单未派数量(悲观锁+兜底 UPDATE 双保险)、同线同班次不超产能、
 * 未齐套可派工但透出警示、下发后调整必填原因。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public interface DispatchOrderService {

    /**
     * 创建派工单，锁工单行防并发超派，成功累加工单已派数量。
     *
     * @param reqVO 创建请求
     * @return 新派工单主键
     */
    Long createDispatch(DispatchSaveReqVO reqVO);

    /**
     * 排产建议(只读不落库)：按交期内工作日×启用产线×班次的剩余产能贪心填充。
     *
     * @param workOrderId 工单主键
     * @return 建议列表，无可行排产时为空集合
     */
    List<DispatchSuggestRespVO> suggestDispatch(Long workOrderId);

    /**
     * 分页查询派工单。
     *
     * @param reqVO 分页请求
     * @return 分页结果
     */
    PageResult<DispatchRespVO> getDispatchPage(DispatchPageReqVO reqVO);

    /**
     * 查询派工单详情。
     *
     * @param id 派工单主键
     * @return 派工单详情
     */
    DispatchRespVO getDispatch(Long id);

    /**
     * 修改派工单：待审核/已审核直接改；已下发必填调整原因并记快照日志；
     * 数量变化同步调整工单已派数量(锁工单行)。
     *
     * @param id    派工单主键
     * @param reqVO 修改请求
     */
    void updateDispatch(Long id, DispatchSaveReqVO reqVO);

    /**
     * 审核派工单：待审核 → 已审核。
     *
     * @param id 派工单主键
     */
    void auditDispatch(Long id);

    /**
     * 下发派工单：已审核 → 已下发(推送现场由现场模块拉取，本模块只置状态)。
     *
     * @param id 派工单主键
     */
    void issueDispatch(Long id);

    /**
     * 取消派工单：待审核/已审核/已下发 → 已取消，回退工单已派数量。
     *
     * @param id     派工单主键
     * @param reason 取消原因
     */
    void cancelDispatch(Long id, String reason);

    /**
     * 查询派工单调整日志，最新在前。
     *
     * @param id 派工单主键
     * @return 调整日志列表，无数据时为空集合
     */
    List<DispatchAdjustLogRespVO> getAdjustLogs(Long id);

    /**
     * 产线排程视图：产线在日期区间内的派工单(排除已取消)。
     *
     * @param lineId    产线主键
     * @param startDate 起始日期(含)
     * @param endDate   结束日期(含)
     * @return 派工单列表，按日期/班次升序
     */
    List<DispatchRespVO> getLineSchedule(Long lineId, LocalDate startDate, LocalDate endDate);
}
