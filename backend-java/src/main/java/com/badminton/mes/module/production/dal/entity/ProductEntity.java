package com.badminton.mes.module.production.dal.entity;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 产品实体，对应表 base_product。
 *
 * <p>基础资料模块尚未建设，本实体仅映射生产订单模块创建工单时需要读取的列。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
@Data
@Entity
@Table(name = "base_product")
public class ProductEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 产品编码 */
    @Column(name = "product_code")
    private String productCode;

    /** 产品名称 */
    @Column(name = "product_name")
    private String productName;

    /** 规格型号 */
    @Column(name = "spec")
    private String spec;

    /** 计量单位 id */
    @Column(name = "unit_id")
    private Long unitId;

    /** 状态：1 启用 0 停用 */
    @Column(name = "status")
    private Integer status;

    /** 逻辑删除标记，映射列 is_deleted */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
