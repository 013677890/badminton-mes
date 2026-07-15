package com.badminton.mes.module.barcode.dal.redis;

import com.badminton.mes.module.barcode.enums.BarcodeSerialResetCycleEnum;

/**
 * 条码流水号发号器接口，屏蔽发号实现供 Service 与单测替换。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public interface BarcodeSerialSequence {

    /**
     * 取指定规则与作用域下的下一个流水号。
     *
     * <p>流水只保证趋势递增与低冲突，不承诺绝对无洞；条码最终唯一性
     * 由 barcode 表唯一索引兜底(已冻结决策)。
     *
     * @param ruleId 条码规则主键
     * @param scope  流水作用域(周期日期段 + 对象编码)
     * @param cycle  流水重置周期，决定计数 Key TTL
     * @return 下一个流水号，从 1 开始
     */
    long next(Long ruleId, String scope, BarcodeSerialResetCycleEnum cycle);
}
