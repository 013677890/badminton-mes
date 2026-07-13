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

    /** 来源系统 */
    private String sourceSystem;

    /** 总数 */
    private int totalCount;

    /** 成功数（进入待确认区且校验通过） */
    private int successCount;

    /** 失败数（校验未通过，进入待确认区异常状态） */
    private int failureCount;

    /** 重复数 */
    private int duplicateCount;

    /** 待确认数据列表 */
    private List<ErpCraftPendingRespVO> pendingItems;
}
