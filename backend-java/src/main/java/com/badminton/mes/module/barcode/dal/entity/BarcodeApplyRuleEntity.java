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
 * 条码应用规则实体，对应表 barcode_apply_rule(V2026071201)。
 *
 * <p>将产品/物料、条码类型、条码规则和标签模板组合成生成入口；
 * "同对象同类型仅一条启用默认规则"由数据库生成列唯一索引 uk_active_default 兜底。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
@Entity
@DynamicInsert
@Table(name = "barcode_apply_rule")
public class BarcodeApplyRuleEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 对象类型：1 产品 2 物料 */
    @Column(name = "object_type", columnDefinition = "tinyint unsigned")
    private Integer objectType;

    /** 适用产品 id(对象类型=1 时必填) */
    @Column(name = "product_id")
    private Long productId;

    /** 适用物料 id(对象类型=2 时必填) */
    @Column(name = "material_id")
    private Long materialId;

    /** 条码类型 id */
    @Column(name = "barcode_type_id")
    private Long barcodeTypeId;

    /** 条码模式：1 唯一码 2 批次码(第一阶段以批次码为主) */
    @Column(name = "barcode_mode", columnDefinition = "tinyint unsigned")
    private Integer barcodeMode;

    /** 条码规则 id，来源=规则生成时必填 */
    @Column(name = "rule_id")
    private Long ruleId;

    /** 标签模板 id */
    @Column(name = "template_id")
    private Long templateId;

    /** 条码来源：1 规则生成 2 传入值生成 3 外部导入 */
    @Column(name = "source_type", columnDefinition = "tinyint unsigned")
    private Integer sourceType;

    /** 是否默认规则：1 是 0 否，映射列 is_default */
    @Column(name = "is_default", columnDefinition = "tinyint unsigned")
    private Boolean defaultFlag;

    /** 规则版本 */
    @Column(name = "version")
    private String version;

    /** 状态：1 启用 0 停用 */
    @Column(name = "status", columnDefinition = "tinyint unsigned")
    private Integer status;

    /** 启用默认规则的对象 id，数据库 STORED 生成列，只读(唯一索引 uk_active_default 依据) */
    @Column(name = "active_default_object_id", insertable = false, updatable = false)
    private Long activeDefaultObjectId;

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
