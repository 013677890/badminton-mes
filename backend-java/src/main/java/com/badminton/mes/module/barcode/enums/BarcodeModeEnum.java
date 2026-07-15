package com.badminton.mes.module.barcode.enums;

import lombok.Getter;

/**
 * 条码模式枚举，对应 barcode_apply_rule.barcode_mode 与 barcode.barcode_mode。
 *
 * <p>第一阶段以批次码"一批一码"为主(已冻结决策)。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Getter
public enum BarcodeModeEnum {

    /** 唯一码：一物一码，单件追溯 */
    UNIQUE(1, "唯一码"),

    /** 批次码：一批一码，批次追溯 */
    BATCH(2, "批次码");

    /** 模式值，与数据库 barcode_mode 字段取值一致 */
    private final Integer mode;

    /** 模式描述 */
    private final String description;

    BarcodeModeEnum(Integer mode, String description) {
        this.mode = mode;
        this.description = description;
    }
}
