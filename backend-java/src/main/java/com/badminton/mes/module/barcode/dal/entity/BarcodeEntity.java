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
 * 条码主表实体，对应表 barcode(V2026071201)。
 *
 * <p>条码值全局唯一(uk_barcode_value)；持久状态仅未使用/已使用/已作废，
 * "已打印"由打印记录派生。工单/任务为逻辑引用。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
@Entity
@DynamicInsert
@Table(name = "barcode")
public class BarcodeEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 条码值，全局唯一 */
    @Column(name = "barcode_value")
    private String barcodeValue;

    /** 条码类型 id */
    @Column(name = "barcode_type_id")
    private Long barcodeTypeId;

    /** 条码模式：1 唯一码 2 批次码 */
    @Column(name = "barcode_mode", columnDefinition = "tinyint unsigned")
    private Integer barcodeMode;

    /** 来源应用规则 id，规则生成时记录 */
    @Column(name = "apply_rule_id")
    private Long applyRuleId;

    /** 产品 id */
    @Column(name = "product_id")
    private Long productId;

    /** 物料 id(材料码) */
    @Column(name = "material_id")
    private Long materialId;

    /** 批次号 */
    @Column(name = "batch_no")
    private String batchNo;

    /** 关联生产工单 id(逻辑引用 prod_work_order) */
    @Column(name = "work_order_id")
    private Long workOrderId;

    /** 关联生产任务单 id(逻辑引用，任务表由 M2 落地) */
    @Column(name = "task_id")
    private Long taskId;

    /** 来源：1 规则生成 2 传入值 3 外部导入 */
    @Column(name = "source_type", columnDefinition = "tinyint unsigned")
    private Integer sourceType;

    /** 状态：0 未使用 1 已使用 2 已作废(已使用不可作废) */
    @Column(name = "barcode_status", columnDefinition = "tinyint unsigned")
    private Integer barcodeStatus;

    /** 创建人用户 id */
    @Column(name = "create_by")
    private Long createBy;

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
