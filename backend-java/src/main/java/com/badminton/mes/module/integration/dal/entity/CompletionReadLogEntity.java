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
 * <p>每次外部分页读取实际返回一条完工单就生成一条日志，记录消费来源和登录用户。日志保存完工
 * 单号等冗余字段，保证后续审计不依赖完工主表仍可用或未被改写。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
@Entity
@DynamicInsert
@Table(name = "integration_completion_read_log")
public class CompletionReadLogEntity {

    /** 数据库自增主键。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 被读取完工单主键，用于关联发布快照。 */
    @Column(name = "completion_order_id")
    private Long completionOrderId;

    /** 被读取完工单号快照，便于按业务编号审计。 */
    @Column(name = "completion_no")
    private String completionNo;

    /** 被读取完工对应的生产工单号快照。 */
    @Column(name = "work_order_no")
    private String workOrderNo;

    /** 发起读取的外部来源系统，按接口协议规范化保存。 */
    @Column(name = "source_system")
    private String sourceSystem;

    /** 触发本次读取的系统用户主键。 */
    @Column(name = "read_by")
    private Long readBy;

    /** 数据库生成的实际读取时间，作为读取日志时间范围筛选字段。 */
    @Column(name = "read_time", insertable = false, updatable = false)
    private LocalDateTime readTime;

    /** 日志记录创建时间，由数据库维护。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 日志最后更新时间，由数据库维护。 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记，正常审计查询排除已删除日志。 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
