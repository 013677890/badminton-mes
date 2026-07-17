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
 * 条码模板实体，对应表 barcode_template(V2026071201)。
 *
 * <p>同一模板编码的多个版本各占一行，(template_code, version) 唯一；
 * 已被应用规则绑定的模板修改时生成新版本行，旧版本保留供打印历史追溯。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
@Entity
@DynamicInsert
@Table(name = "barcode_template")
public class BarcodeTemplateEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 模板编码，同编码多版本共用 */
    @Column(name = "template_code")
    private String templateCode;

    /** 模板名称 */
    @Column(name = "template_name")
    private String templateName;

    /** 纸张宽度(mm) */
    @Column(name = "paper_width")
    private BigDecimal paperWidth;

    /** 纸张高度(mm) */
    @Column(name = "paper_height")
    private BigDecimal paperHeight;

    /** 模板版本，系统管理(V1 起，被绑定后修改升版本) */
    @Column(name = "version")
    private String version;

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
