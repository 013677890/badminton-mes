package com.badminton.mes.module.barcode.dal.entity;

import org.hibernate.annotations.Immutable;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 产品只读引用实体，映射 A 组表 base_product 的 B 组视角最小字段。
 *
 * <p>协作边界约定：B 组可直接只读查询 A 组基础数据，但必须转成自己的
 * DTO/VO 且不得写入。{@code @Immutable} 由 Hibernate 保证只读。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
@Entity(name = "BarcodeProductRef")
@Immutable
@Table(name = "base_product")
public class ProductRefEntity {

    /** 主键 */
    @Id
    private Long id;

    /** 产品编码，条码规则 productCode 变量取值 */
    @Column(name = "product_code", insertable = false, updatable = false)
    private String productCode;

    /** 产品名称 */
    @Column(name = "product_name", insertable = false, updatable = false)
    private String productName;

    /** 状态：1 启用 0 停用 */
    @Column(name = "status", insertable = false, updatable = false,
            columnDefinition = "tinyint unsigned")
    private Integer status;

    /** 逻辑删除标记，映射列 is_deleted */
    @Column(name = "is_deleted", insertable = false, updatable = false,
            columnDefinition = "tinyint unsigned")
    private Boolean deleted;
}
