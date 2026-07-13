package com.badminton.mes.module.integration.controller.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 单条物料库存与在途快照。
 *
 * @author Codex
 * @date 2026/07/13
 */
@Data
public class MaterialStockItemReqVO {

    @NotBlank(message = "物料编码不能为空")
    @Size(max = 32, message = "物料编码长度不能超过 32")
    private String materialCode;

    @NotNull(message = "可用数量不能为空")
    @DecimalMin(value = "0", message = "可用数量不能小于 0")
    private BigDecimal availableQuantity;

    @NotNull(message = "锁定数量不能为空")
    @DecimalMin(value = "0", message = "锁定数量不能小于 0")
    private BigDecimal lockedQuantity;

    @NotNull(message = "在检数量不能为空")
    @DecimalMin(value = "0", message = "在检数量不能小于 0")
    private BigDecimal checkingQuantity;

    @NotNull(message = "在途数量不能为空")
    @DecimalMin(value = "0", message = "在途数量不能小于 0")
    private BigDecimal transitQuantity;

    @NotNull(message = "同步时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime syncTime;
}
