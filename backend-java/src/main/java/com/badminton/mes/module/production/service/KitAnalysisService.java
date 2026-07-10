package com.badminton.mes.module.production.service;

import java.util.List;

import com.badminton.mes.module.production.controller.vo.KitAnalysisRespVO;
import com.badminton.mes.module.production.controller.vo.ShortageBoardRespVO;
import com.badminton.mes.module.production.controller.vo.ShortageHandleRespVO;
import com.badminton.mes.module.production.controller.vo.ShortageHandleSaveReqVO;
import com.badminton.mes.module.production.controller.vo.ShortageOrderRespVO;

/**
 * 齐套分析 Service 接口。
 *
 * <p>业务规则见 wiki/16-齐套分析与派工单设计.md 与 wiki/01-生产订单需求分析.md §2。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public interface KitAnalysisService {

    /**
     * 对工单执行(重新)齐套分析：软删旧结果、按库存快照逐物料计算、
     * 回写工单齐套状态，全程单事务。
     *
     * @param workOrderId 工单主键
     * @return 工单级齐套状态(1 齐套 2 部分齐套 3 欠料)
     */
    Integer analyzeWorkOrder(Long workOrderId);

    /**
     * 查询工单最新一次齐套分析结果。
     *
     * @param workOrderId 工单主键
     * @return 逐物料分析结果，未分析时为空集合
     */
    List<KitAnalysisRespVO> getKitResult(Long workOrderId);

    /**
     * 欠料看板：按物料汇总欠料量、影响工单数、在途与预计到料。
     *
     * @return 看板汇总列表，欠料量降序，无欠料时为空集合
     */
    List<ShortageBoardRespVO> getShortageBoard();

    /**
     * 欠料看板下钻：某物料影响的工单明细。
     *
     * @param materialId 物料主键
     * @return 欠料工单行列表，无数据时为空集合
     */
    List<ShortageOrderRespVO> getShortageOrdersByMaterial(Long materialId);

    /**
     * 新增欠料处理记录。
     *
     * @param reqVO 处理记录请求
     * @return 新记录主键
     */
    Long createShortageHandle(ShortageHandleSaveReqVO reqVO);

    /**
     * 标记欠料处理记录为已解决，重复解决报错。
     *
     * @param id 处理记录主键
     */
    void resolveShortageHandle(Long id);

    /**
     * 查询工单的欠料处理记录，最新在前。
     *
     * @param workOrderId 工单主键
     * @return 处理记录列表，无数据时为空集合
     */
    List<ShortageHandleRespVO> getShortageHandles(Long workOrderId);
}
