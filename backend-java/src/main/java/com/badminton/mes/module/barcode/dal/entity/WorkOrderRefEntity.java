package com.badminton.mes.module.barcode.dal.entity;

import org.hibernate.annotations.Immutable;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 生产工单只读引用实体，映射 A 组表 prod_work_order 的 B 组视角最小字段。
 *
 * <p>条码生成关联工单时校验存在性、产品一致性与车间数据范围；
 * {@code @Immutable} 由 Hibernate 保证只读，B 组不写 A 组工单计划字段。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
@Entity(name = "BarcodeWorkOrderRef")
@Immutable
@Table(name = "prod_work_order")
public class WorkOrderRefEntity {

    /** 主键 */
    @Id
    private Long id;

    /** 工单号 */
    @Column(name = "work_order_no", insertable = false, updatable = false)
    private String workOrderNo;

    /** 产品 id */
    @Column(name = "product_id", insertable = false, updatable = false)
    private Long productId;

    /** 生产批次号 */
    @Column(name = "batch_no", insertable = false, updatable = false)
    private String batchNo;

    /** 目标车间 id，数据范围校验依据 */
    @Column(name = "workshop_id", insertable = false, updatable = false)
    private Long workshopId;

    /** 工单状态 */
    @Column(name = "order_status", insertable = false, updatable = false,
            columnDefinition = "tinyint unsigned")
    private Integer orderStatus;

    /** 逻辑删除标记，映射列 is_deleted */
    @Column(name = "is_deleted", insertable = false, updatable = false,
            columnDefinition = "tinyint unsigned")
    private Boolean deleted;
}
