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
 * 条码规则组成明细实体，对应表 barcode_rule_item(V2026071201)。
 *
 * <p>规则明细单独建表，避免把复杂 JSON 作为唯一事实数据源；
 * 同规则 seq 唯一(uk_rule_seq)。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
@Entity
@DynamicInsert
@Table(name = "barcode_rule_item")
public class BarcodeRuleItemEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属条码规则 id */
    @Column(name = "rule_id")
    private Long ruleId;

    /** 组成顺序，同规则内唯一 */
    @Column(name = "seq", columnDefinition = "tinyint unsigned")
    private Integer seq;

    /** 组成类型：1 常量 2 日期 3 变量(产品编码/产线编码) 4 流水号 */
    @Column(name = "item_type", columnDefinition = "tinyint unsigned")
    private Integer itemType;

    /** 常量值或变量名 */
    @Column(name = "item_value")
    private String itemValue;

    /** 日期格式(yyyyMMdd) */
    @Column(name = "date_format")
    private String dateFormat;

    /** 该段长度 */
    @Column(name = "item_length", columnDefinition = "tinyint unsigned")
    private Integer itemLength;

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
