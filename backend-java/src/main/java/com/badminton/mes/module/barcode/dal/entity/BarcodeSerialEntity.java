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
 * 条码流水号记录实体，对应表 barcode_serial(V2026071201)。
 *
 * <p>MySQL 侧的流水事实兜底：Redis INCR 发号后同步落库，
 * (rule_id, serial_scope) 唯一(uk_rule_scope，完整列索引)；
 * Redis Key 丢失时以本表 current_serial 恢复计数。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
@Entity
@DynamicInsert
@Table(name = "barcode_serial")
public class BarcodeSerialEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 条码规则 id */
    @Column(name = "rule_id")
    private Long ruleId;

    /** 流水维度值(日期段+产品等组合，≤64 字符)，与规则联合唯一 */
    @Column(name = "serial_scope")
    private String serialScope;

    /** 当前流水号 */
    @Column(name = "current_serial", columnDefinition = "int unsigned")
    private Long currentSerial;

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
