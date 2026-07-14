package com.badminton.mes.module.integration.dal.entity;

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
 * 生产完工单逐条读取日志实体，对应 integration_completion_read_log。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
@Entity
@DynamicInsert
@Table(name = "integration_completion_read_log")
public class CompletionReadLogEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 被读取完工单主键 */
    @Column(name = "completion_order_id")
    private Long completionOrderId;

    /** 被读取完工单号 */
    @Column(name = "completion_no")
    private String completionNo;

    /** 生产工单号 */
    @Column(name = "work_order_no")
    private String workOrderNo;

    /** 读取来源系统 */
    @Column(name = "source_system")
    private String sourceSystem;

    /** 调用用户 */
    @Column(name = "read_by")
    private Long readBy;

    /** 读取时间 */
    @Column(name = "read_time", insertable = false, updatable = false)
    private LocalDateTime readTime;

    /** 创建时间 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
