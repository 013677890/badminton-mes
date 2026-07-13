package com.badminton.mes.module.production.dal.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

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
@DynamicInsert
@DynamicUpdate
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

    /** 产品类型：1 成品 2 半成品 */
    @Column(name = "product_type")
    private Integer productType;

    /** 产品等级 */
    @Column(name = "grade")
    private String grade;

    /** 计量单位 id */
    @Column(name = "unit_id")
    private Long unitId;

    /** 状态：1 启用 0 停用 */
    @Column(name = "status")
    private Integer status;

    /** 创建人 */
    @Column(name = "create_by")
    private Long createBy;

    /** 最后修改人 */
    @Column(name = "update_by")
    private Long updateBy;

    /** 乐观锁版本 */
    @Version
    @Column(name = "version")
    private Integer version;

    /** 创建时间 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记，映射列 is_deleted */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
