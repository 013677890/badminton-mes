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
 * 工单状态日志实体，对应表 prod_work_order_status_log。
 *
 * <p>记录状态流转与下达后的计划变更，满足"工单状态变化有日志可查"的验收标准；
 * 恢复(resume)时从最近一条暂停日志取 fromStatus 还原暂停前状态。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
@Data
@Entity
@DynamicInsert
@Table(name = "prod_work_order_status_log")
public class WorkOrderStatusLogEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 生产工单 id */
    @Column(name = "work_order_id")
    private Long workOrderId;

    /** 变更前状态(计划变更时与 toStatus 相同) */
    @Column(name = "from_status", columnDefinition = "tinyint unsigned")
    private Integer fromStatus;

    /** 变更后状态 */
    @Column(name = "to_status", columnDefinition = "tinyint unsigned")
    private Integer toStatus;

    /** 变更类型：1 状态流转 2 计划变更 */
    @Column(name = "change_type", columnDefinition = "tinyint unsigned")
    private Integer changeType;

    /** 变更原因 */
    @Column(name = "change_reason")
    private String changeReason;

    /** 操作人用户 id */
    @Column(name = "operate_by")
    private Long operateBy;

    /** 操作时间 */
    @Column(name = "operate_time")
    private LocalDateTime operateTime;

    /** 逻辑删除标记，映射列 is_deleted */
    @Column(name = "is_deleted", columnDefinition = "tinyint unsigned")
    private Boolean deleted;
}
