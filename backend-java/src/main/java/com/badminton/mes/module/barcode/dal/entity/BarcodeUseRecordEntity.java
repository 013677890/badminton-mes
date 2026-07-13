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
 * 条码使用记录实体，对应表 barcode_use_record(V2026071201，2026-07-11 §3.4 最小契约)。
 *
 * <p>扫码使用事实：关联条码、任务、工序、人员、设备与业务发生时间。
 * M1 仅提供查询；写入随 M2 现场执行扫码切片落地。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
@Entity
@DynamicInsert
@Table(name = "barcode_use_record")
public class BarcodeUseRecordEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 条码 id */
    @Column(name = "barcode_id")
    private Long barcodeId;

    /** 生产任务单 id(逻辑引用，任务表由 M2 落地) */
    @Column(name = "task_id")
    private Long taskId;

    /** 工序 id(逻辑引用，工艺表由 A/公共迁移落地) */
    @Column(name = "process_id")
    private Long processId;

    /** 扫码人员用户 id */
    @Column(name = "user_id")
    private Long userId;

    /** 设备 id，可空 */
    @Column(name = "equipment_id")
    private Long equipmentId;

    /** 使用类型：1 工序开工扫码 2 工序完工扫码 3 报工扫码 4 其他 */
    @Column(name = "use_type", columnDefinition = "tinyint unsigned")
    private Integer useType;

    /** 业务发生时间 */
    @Column(name = "business_time")
    private LocalDateTime businessTime;

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
