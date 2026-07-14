package com.badminton.mes.module.integration.controller.vo;

import java.util.List;

import lombok.Data;

/**
 * ERP 生产任务单同步结果。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
public class ErpTaskSyncRespVO {

    /** 来源系统 */
    private String sourceSystem;

    /** 总数 */
    private int totalCount;

    /** 成功数 */
    private int successCount;

    /** 失败数 */
    private int failureCount;

    /** 重复数 */
    private int duplicateCount;

    /** 逐条处理明细 */
    private List<Detail> details;

    /**
     * 单条 ERP 任务同步处理明细。
     */
    @Data
    public static class Detail {

        /** ERP 任务单号 */
        private String erpOrderNo;

        /** 处理状态：SUCCESS / FAILED / DUPLICATE */
        private String status;

        /** 生成的 MES 工单主键（成功时） */
        private Long workOrderId;

        /** 生成的 MES 工单号（成功时） */
        private String workOrderNo;

        /** 失败错误码（失败时） */
        private String errorCode;

        /** 失败原因（失败时） */
        private String errorMessage;
    }
}
