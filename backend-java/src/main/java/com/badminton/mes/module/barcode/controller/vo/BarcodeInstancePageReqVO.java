package com.badminton.mes.module.barcode.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 条码实例分页查询请求 VO，继承 {@link PageParam} 获得分页参数与入参保护。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BarcodeInstancePageReqVO extends PageParam {

    /** 条码值前缀，右模糊匹配，可空 */
    @Size(max = 64, message = "条码值长度不能超过 64")
    private String barcodeValue;

    /** 批次号前缀，右模糊匹配，可空 */
    @Size(max = 64, message = "批次号长度不能超过 64")
    private String batchNo;

    /** 条码类型 id，可空 */
    @Positive(message = "条码类型 id 必须为正数")
    private Long barcodeTypeId;

    /** 关联生产工单 id，可空 */
    @Positive(message = "工单 id 必须为正数")
    private Long workOrderId;

    /** 关联生产任务单 id，可空 */
    @Positive(message = "任务 id 必须为正数")
    private Long taskId;

    /** 来源：1 规则生成 2 传入值 3 外部导入，可空 */
    @Min(value = 1, message = "来源取值为 1-3")
    @Max(value = 3, message = "来源取值为 1-3")
    private Integer sourceType;

    /** 状态：0 未使用 1 已使用 2 已作废，可空 */
    @Min(value = 0, message = "状态取值为 0-2")
    @Max(value = 2, message = "状态取值为 0-2")
    private Integer barcodeStatus;
}
