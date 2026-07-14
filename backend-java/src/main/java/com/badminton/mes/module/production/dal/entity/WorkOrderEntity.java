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
 * 生产工单实体，对应表 prod_work_order。
 *
 * <p>实体只承载数据库状态，不建立 JPA 级联关系，不调用 Service 或 Redis。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
@Data
@Entity
@DynamicInsert
@Table(name = "prod_work_order")
public class WorkOrderEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 工单号 */
    @Column(name = "work_order_no")
    private String workOrderNo;

    /** 来源：1 手工 2 导入 3 ERP同步 4 API写入 */
    @Column(name = "source_type", columnDefinition = "tinyint unsigned")
    private Integer sourceType;

    /** 外部来源系统 */
    @Column(name = "source_system")
    private String sourceSystem;

    /** 外部来源单号 */
    @Column(name = "source_order_no")
    private String sourceOrderNo;

    /** 产品 id */
    @Column(name = "product_id")
    private Long productId;

    /** 产品名称 */
    @Column(name = "product_name")
    private String productName;

    /** 规格型号 */
    @Column(name = "spec")
    private String spec;

    /** 计量单位 id */
    @Column(name = "unit_id")
    private Long unitId;

    /** 生产批次号 */
    @Column(name = "batch_no")
    private String batchNo;

    /** BOM 版本 id */
    @Column(name = "bom_id")
    private Long bomId;

    /** 工艺路线 id */
    @Column(name = "routing_id")
    private Long routingId;

    /** 客户 id */
    @Column(name = "customer_id")
    private Long customerId;

    /** 目标车间 id */
    @Column(name = "workshop_id")
    private Long workshopId;

    /** 计划数量 */
    @Column(name = "plan_quantity")
    private Integer planQuantity;

    /** 已派工数量 */
    @Column(name = "dispatched_quantity")
    private Integer dispatchedQuantity;

    /** 投入数量 */
    @Column(name = "input_quantity")
    private Integer inputQuantity;

    /** 完工数量 */
    @Column(name = "finish_quantity")
    private Integer finishQuantity;

    /** 不良数量 */
    @Column(name = "defect_quantity")
    private Integer defectQuantity;

    /** 返修数量 */
    @Column(name = "rework_quantity")
    private Integer reworkQuantity;

    /** 允许超产比例 */
    @Column(name = "over_ratio", precision = 5, scale = 2)
    private BigDecimal overRatio;

    /** 优先级 */
    @Column(name = "priority", columnDefinition = "tinyint unsigned")
    private Integer priority;

    /** 计划开始时间 */
    @Column(name = "plan_start_time")
    private LocalDateTime planStartTime;

    /** 计划完成时间 */
    @Column(name = "plan_end_time")
    private LocalDateTime planEndTime;

    /** 工单状态 */
    @Column(name = "order_status", columnDefinition = "tinyint unsigned")
    private Integer orderStatus;

    /** 齐套状态 */
    @Column(name = "kit_status", columnDefinition = "tinyint unsigned")
    private Integer kitStatus;

    /** 创建人用户 id */
    @Column(name = "create_by")
    private Long createBy;

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
