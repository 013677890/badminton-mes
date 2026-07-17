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

    /** 本次批量同步实际使用的来源系统。 */
    private String sourceSystem;

    /** 经过门面筛选、实际参与处理的任务总数。 */
    private int totalCount;

    /** 新建 MES 工单成功的任务数量。 */
    private int successCount;

    /** 业务校验或持久化失败的任务数量。 */
    private int failureCount;

    /** 已存在 MES 工单、未重复创建的任务数量。 */
    private int duplicateCount;

    /** 每条任务的成功、失败或重复处理结果。 */
    private List<Detail> details;

    /**
     * 单条 ERP 任务同步处理明细。
     */
    @Data
    public static class Detail {

        /** ERP 任务业务编号。 */
        private String erpOrderNo;

        /** 处理状态：SUCCESS、FAILED 或 DUPLICATE。 */
        private String status;

        /** 成功或重复时关联的 MES 工单主键。 */
        private Long workOrderId;

        /** 成功或重复时关联的 MES 工单号。 */
        private String workOrderNo;

        /** 失败错误码（失败时） */
        private String errorCode;

        /** 失败原因（失败时） */
        private String errorMessage;
    }
}
