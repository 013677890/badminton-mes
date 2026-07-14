package com.badminton.mes.module.production.dal.entity;

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
 * 物料库存可用量实体，对应表 material_stock。
 *
 * <p>一物料一条快照，数据由 WMS/ERP 批量同步。
 * 齐套分析只读该表，不做库存扣减。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
@Entity
@DynamicInsert
@Table(name = "material_stock")
public class MaterialStockEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 物料 id(唯一) */
    @Column(name = "material_id")
    private Long materialId;

    /** 来源系统 */
    @Column(name = "source_system")
    private String sourceSystem;

    /** 可用数量 */
    @Column(name = "available_quantity")
    private BigDecimal availableQuantity;

    /** 已锁定数量 */
    @Column(name = "locked_quantity")
    private BigDecimal lockedQuantity;

    /** 在检数量 */
    @Column(name = "checking_quantity")
    private BigDecimal checkingQuantity;

    /** 在途数量 */
    @Column(name = "transit_quantity")
    private BigDecimal transitQuantity;

    /** 库存同步时间(来自 WMS/ERP) */
    @Column(name = "sync_time")
    private LocalDateTime syncTime;

    /** 创建时间 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time", insertable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记，映射列 is_deleted */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
