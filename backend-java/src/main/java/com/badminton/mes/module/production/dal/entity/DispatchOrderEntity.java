package com.badminton.mes.module.production.dal.entity;

import java.time.LocalDate;
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
 * 派工单实体，对应表 dispatch_order。
 *
 * <p>排产结果单据：一个工单可拆多张派工单(不同产线/日期/班次)，
 * 派工数量合计不超工单"计划数量×(1+超产比例)"，由工单行锁+兜底 UPDATE 保证。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "dispatch_order")
public class DispatchOrderEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 派工单号(唯一) */
    @Column(name = "dispatch_no")
    private String dispatchNo;

    /** 来源生产工单 id */
    @Column(name = "work_order_id")
    private Long workOrderId;

    /** 产线 id */
    @Column(name = "line_id")
    private Long lineId;

    /** 班次 id */
    @Column(name = "shift_id")
    private Long shiftId;

    /** 排产日期 */
    @Column(name = "plan_date")
    private LocalDate planDate;

    /** 计划数量(不超工单未派数量) */
    @Column(name = "plan_quantity")
    private Integer planQuantity;

    /** 计划开始时间 */
    @Column(name = "plan_start_time")
    private LocalDateTime planStartTime;

    /** 计划结束时间 */
    @Column(name = "plan_end_time")
    private LocalDateTime planEndTime;

    /** 是否系统建议排产：1 是 0 人工 */
    @Column(name = "is_suggest")
    private Integer suggest;

    /** 状态：0 待审核 1 已审核 2 已下发 3 执行中 4 已完成 5 已取消 */
    @Column(name = "dispatch_status")
    private Integer dispatchStatus;

    /** 审核人 */
    @Column(name = "audit_by")
    private Long auditBy;

    /** 审核时间 */
    @Column(name = "audit_time")
    private LocalDateTime auditTime;

    /** 下发后调整原因 */
    @Column(name = "adjust_reason")
    private String adjustReason;

    /** 创建人 */
    @Column(name = "create_by")
    private Long createBy;

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
