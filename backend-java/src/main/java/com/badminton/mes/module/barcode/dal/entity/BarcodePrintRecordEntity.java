package com.badminton.mes.module.barcode.dal.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 条码打印记录实体，对应表 barcode_print_record(V2026071201)。
 *
 * <p>逐次插入模型：每次打印插入一行，print_count 为同一条码的打印序号，
 * (barcode_id, print_count) 唯一；模板版本与预览快照保证打印历史可复现
 * (2026-07-11 契约差异登记 §3.2)。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
@Entity
@DynamicInsert
@Table(name = "barcode_print_record")
public class BarcodePrintRecordEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 条码 id */
    @Column(name = "barcode_id")
    private Long barcodeId;

    /** 打印模板 id(具体版本行) */
    @Column(name = "template_id")
    private Long templateId;

    /** 打印时模板版本快照 */
    @Column(name = "template_version")
    private String templateVersion;

    /** 打印时预览内容快照(JSON) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preview_content")
    private String previewContent;

    /** 打印人用户 id */
    @Column(name = "print_by")
    private Long printBy;

    /** 同一条码的打印序号，从 1 递增 */
    @Column(name = "print_count")
    private Integer printCount;

    /** 重复打印原因，首次打印为空 */
    @Column(name = "reprint_reason")
    private String reprintReason;

    /** 打印时间 */
    @Column(name = "print_time")
    private LocalDateTime printTime;

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
