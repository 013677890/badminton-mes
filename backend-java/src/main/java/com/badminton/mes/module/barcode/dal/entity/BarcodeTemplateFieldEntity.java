package com.badminton.mes.module.barcode.dal.entity;

import java.math.BigDecimal;
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
 * 条码模板字段实体，对应表 barcode_template_field(V2026071201)。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
@Entity
@DynamicInsert
@Table(name = "barcode_template_field")
public class BarcodeTemplateFieldEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属条码模板 id(具体版本行) */
    @Column(name = "template_id")
    private Long templateId;

    /** 字段名称(条码值/产品名称/批次号/生产日期/工单号) */
    @Column(name = "field_name")
    private String fieldName;

    /** 字段类型：1 文本 2 条码 3 二维码 */
    @Column(name = "field_type", columnDefinition = "tinyint unsigned")
    private Integer fieldType;

    /** 数据来源字段 */
    @Column(name = "data_source")
    private String dataSource;

    /** X 位置(mm) */
    @Column(name = "pos_x")
    private BigDecimal posX;

    /** Y 位置(mm) */
    @Column(name = "pos_y")
    private BigDecimal posY;

    /** 字体大小 */
    @Column(name = "font_size", columnDefinition = "tinyint unsigned")
    private Integer fontSize;

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
