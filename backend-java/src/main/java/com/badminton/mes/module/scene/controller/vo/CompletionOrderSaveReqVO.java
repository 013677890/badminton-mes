package com.badminton.mes.module.scene.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 生产完工单创建请求。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
public class CompletionOrderSaveReqVO {

    @NotNull(message = "现场生产任务不能为空")
    private Long productionTaskId;

    /** 兼容旧调用方；服务端以现场生产任务所属工单为准。 */
    private Long workOrderId;

    @NotBlank(message = "产品批次不能为空")
    @Size(max = 64, message = "产品批次长度不能超过 64")
    private String batchNo;

    @NotNull(message = "完工数量不能为空")
    @Positive(message = "完工数量必须大于 0")
    private Integer completionQuantity;

    @NotNull(message = "良品数量不能为空")
    @PositiveOrZero(message = "良品数量不能小于 0")
    private Integer goodQuantity;

    @NotNull(message = "不良数量不能为空")
    @PositiveOrZero(message = "不良数量不能小于 0")
    private Integer defectQuantity;
}
