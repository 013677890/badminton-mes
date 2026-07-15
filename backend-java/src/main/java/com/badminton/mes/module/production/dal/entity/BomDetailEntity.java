package com.badminton.mes.module.production.dal.entity;

import java.math.BigDecimal;
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

/**
 * BOM 明细实体，对应表 base_bom_detail。
 *
 * <p>工单下达时按明细计算物料需求：需求数量 = 计划数 × 标准用量 ×(1 + 损耗率)。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "base_bom_detail")
public class BomDetailEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** BOM 主表 id */
    @Column(name = "bom_id")
    private Long bomId;

    /** 物料 id */
    @Column(name = "material_id")
    private Long materialId;

    /** 标准用量(单位产品) */
    @Column(name = "quantity", precision = 12, scale = 4)
    private BigDecimal quantity;

    /** 损耗率(%) */
    @Column(name = "loss_rate", precision = 5, scale = 2)
    private BigDecimal lossRate;

    /** 创建时间 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记，映射列 is_deleted */
    @Column(name = "is_deleted", columnDefinition = "tinyint unsigned")
    private Boolean deleted;
}
