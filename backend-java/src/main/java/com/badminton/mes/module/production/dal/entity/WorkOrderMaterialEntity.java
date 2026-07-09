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
 * 工单物料需求实体，对应表 prod_work_order_material。
 *
 * <p>工单下达时按 BOM 明细批量生成；已领数量由后续领料/发料流程回写。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
@Data
@Entity
@DynamicInsert
@Table(name = "prod_work_order_material")
public class WorkOrderMaterialEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 生产工单 id */
    @Column(name = "work_order_id")
    private Long workOrderId;

    /** 物料 id */
    @Column(name = "material_id")
    private Long materialId;

    /** 需求数量(计划数×BOM用量) */
    @Column(name = "require_quantity")
    private BigDecimal requireQuantity;

    /** 已领/已发数量 */
    @Column(name = "issued_quantity")
    private BigDecimal issuedQuantity;

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
