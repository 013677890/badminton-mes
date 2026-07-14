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
 * 齐套分析结果实体，对应表 kit_analysis。
 *
 * <p>每次分析先软删该工单旧结果再插入新结果(同一事务)，
 * 未删除行即最新一次分析快照。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
@Entity
@DynamicInsert
@Table(name = "kit_analysis")
public class KitAnalysisEntity {

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

    /** 需求数量(需求-已领的剩余需求) */
    @Column(name = "require_quantity")
    private BigDecimal requireQuantity;

    /** 可用数量(已扣锁定与在检) */
    @Column(name = "available_quantity")
    private BigDecimal availableQuantity;

    /** 在途数量 */
    @Column(name = "transit_quantity")
    private BigDecimal transitQuantity;

    /** 欠料数量 */
    @Column(name = "shortage_quantity")
    private BigDecimal shortageQuantity;

    /** 齐套状态：1 齐套 2 部分齐套 3 欠料 */
    @Column(name = "kit_status")
    private Integer kitStatus;

    /** 分析时间 */
    @Column(name = "analysis_time")
    private LocalDateTime analysisTime;

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
