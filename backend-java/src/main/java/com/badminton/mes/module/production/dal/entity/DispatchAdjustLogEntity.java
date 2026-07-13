package com.badminton.mes.module.production.dal.entity;

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
 * 派工单排产调整日志实体，对应表 prod_dispatch_adjust_log。
 *
 * <p>记录建议排产与人工创建/调整/审核/下发/取消全过程，
 * 满足"下发后调整须记录原因"的需求(01-生产订单需求分析 §3)。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
@Entity
@DynamicInsert
@Table(name = "prod_dispatch_adjust_log")
public class DispatchAdjustLogEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 派工单 id */
    @Column(name = "dispatch_order_id")
    private Long dispatchOrderId;

    /** 记录类型：1 系统建议 2 人工创建 3 调整 4 审核 5 下发 6 取消 */
    @Column(name = "adjust_type")
    private Integer adjustType;

    /** 调整前快照(产线/班次/日期/数量 JSON) */
    @Column(name = "before_snapshot")
    private String beforeSnapshot;

    /** 调整后快照(JSON) */
    @Column(name = "after_snapshot")
    private String afterSnapshot;

    /** 调整原因(下发后调整必填) */
    @Column(name = "adjust_reason")
    private String adjustReason;

    /** 操作人 */
    @Column(name = "operator_id")
    private Long operatorId;

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
