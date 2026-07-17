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
 * 条码类型实体，对应表 barcode_type(V2026071201)。
 *
 * <p>实体只承载数据库状态，不建立 JPA 级联关系，不调用 Service 或 Redis。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
@Entity
@DynamicInsert
@Table(name = "barcode_type")
public class BarcodeTypeEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 类型编码，全局唯一 */
    @Column(name = "type_code")
    private String typeCode;

    /** 类型名称：产品码/内外箱码/中箱码/栈板码/材料码 */
    @Column(name = "type_name")
    private String typeName;

    /** 适用对象说明 */
    @Column(name = "apply_object")
    private String applyObject;

    /** 状态：1 启用 0 停用(停用后不可新建应用规则) */
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
