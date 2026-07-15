package com.badminton.mes.module.barcode.dal.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 条码规则实体，对应表 barcode_rule(V2026071201)。
 *
 * <p>规则组成明细见 {@link BarcodeRuleItemEntity}；规则修改只影响新生成条码，
 * 不影响历史条码(已冻结决策)。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
@Entity
@DynamicInsert
@Table(name = "barcode_rule")
public class BarcodeRuleEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 规则编码，全局唯一 */
    @Column(name = "rule_code")
    private String ruleCode;

    /** 规则名称 */
    @Column(name = "rule_name")
    private String ruleName;

    /** 适用条码类型 id */
    @Column(name = "barcode_type_id")
    private Long barcodeTypeId;

    /** 流水号位数，达到 10^位数 - 1 后报规则容量不足，不回绕 */
    @Column(name = "serial_length", columnDefinition = "tinyint unsigned")
    private Integer serialLength;

    /** 流水号重置周期：1 按日 2 按月 3 不重置 */
    @Column(name = "serial_reset_cycle", columnDefinition = "tinyint unsigned")
    private Integer serialResetCycle;

    /** 状态：1 启用 0 停用 */
    @Column(name = "status", columnDefinition = "tinyint unsigned")
    private Integer status;

    /** 创建时间 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time", insertable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记，映射列 is_deleted */
    @Column(name = "is_deleted", columnDefinition = "tinyint unsigned")
    private Boolean deleted;
}
