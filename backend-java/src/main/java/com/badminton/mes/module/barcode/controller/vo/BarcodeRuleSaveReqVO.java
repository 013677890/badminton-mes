package com.badminton.mes.module.barcode.controller.vo;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 条码规则创建/修改请求 VO，携带完整组成明细，修改时整体重写明细。
 *
 * <p>状态不接收前端提交，新建默认启用，启停由独立动作接口流转。
 * 流水位数上限 9：流水号按 10^位数-1 计容量，9 位内可安全落入
 * barcode_serial.current_serial(int unsigned)。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeRuleSaveReqVO {

    /** 规则编码，全局唯一 */
    @NotBlank(message = "规则编码不能为空")
    @Size(max = 32, message = "规则编码长度不能超过 32")
    private String ruleCode;

    /** 规则名称 */
    @NotBlank(message = "规则名称不能为空")
    @Size(max = 64, message = "规则名称长度不能超过 64")
    private String ruleName;

    /** 适用条码类型 id */
    @NotNull(message = "条码类型不能为空")
    @Positive(message = "条码类型 id 必须为正数")
    private Long barcodeTypeId;

    /** 流水号位数，达到 10^位数-1 后报规则容量不足 */
    @NotNull(message = "流水号位数不能为空")
    @Min(value = 1, message = "流水号位数最小值为 1")
    @Max(value = 9, message = "流水号位数最大值为 9")
    private Integer serialLength;

    /** 流水号重置周期：1 按日 2 按月 3 不重置 */
    @NotNull(message = "流水号重置周期不能为空")
    @Min(value = 1, message = "流水号重置周期取值为 1-3")
    @Max(value = 3, message = "流水号重置周期取值为 1-3")
    private Integer serialResetCycle;

    /** 组成明细，按 seq 拼接生成条码 */
    @NotEmpty(message = "规则组成明细不能为空")
    @Valid
    private List<BarcodeRuleItemSaveReqVO> items;
}
