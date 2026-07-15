package com.badminton.mes.module.barcode.controller.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 批次码生成请求 VO。
 *
 * <p>按应用规则生成：规则生成来源自动拼接条码值；传入值生成来源必须
 * 提供 inputBarcodeValue。任务 id 为逻辑引用，任务表由 M2 落地后补充校验。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeGenerateReqVO {

    /** 条码应用规则 id */
    @NotNull(message = "应用规则不能为空")
    @Positive(message = "应用规则 id 必须为正数")
    private Long applyRuleId;

    /** 批次号，可空；批次码模式下缺省取生成的条码值 */
    @Size(max = 64, message = "批次号长度不能超过 64")
    private String batchNo;

    /** 关联生产工单 id，可空；提供时校验存在性、产品一致性与车间数据范围 */
    @Positive(message = "工单 id 必须为正数")
    private Long workOrderId;

    /** 关联生产任务单 id，可空(逻辑引用，M2 落地任务表后补充校验) */
    @Positive(message = "任务 id 必须为正数")
    private Long taskId;

    /** 产线编码，规则含产线编码变量时必填 */
    @Size(max = 64, message = "产线编码长度不能超过 64")
    private String lineCode;

    /** 传入条码值，应用规则来源=传入值生成时必填 */
    @Size(max = 64, message = "条码值长度不能超过 64")
    private String inputBarcodeValue;
}
