package com.badminton.mes.module.integration.controller.vo;

import java.util.List;

import lombok.Data;

/**
 * ERP 工艺数据同步结果。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
public class ErpCraftSyncRespVO {

    /** 本次工艺同步实际使用的来源系统。 */
    private String sourceSystem;

    /** 数据源返回并参与处理的工艺路线总数。 */
    private int totalCount;

    /** 校验通过并进入待确认区的工艺数量。 */
    private int successCount;

    /** 校验失败并保留为异常暂存状态的工艺数量。 */
    private int failureCount;

    /** 已有待确认或已确认记录、未重复覆盖的工艺数量。 */
    private int duplicateCount;

    /** 本次处理产生或命中的待确认记录响应列表。 */
    private List<ErpCraftPendingRespVO> pendingItems;
}
