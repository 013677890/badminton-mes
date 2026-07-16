package com.badminton.mes.module.quality.dal.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 质量检验标准方案实体，每行代表一个不可覆盖的方案版本。
 *
 * <p>同一 {@code planCode} 下可存在多个递增版本。新建方案从草稿开始，草稿可编辑或删除；审核后进入
 * 生效状态并固定审核信息，生效方案可停用；生效或停用版本可以复制出新的草稿版本，而不是覆盖原版本。
 * 这种状态机保证历史检验单引用的方案版本能够长期追溯。产品、客户与检验类型共同描述适用范围，
 * 同一范围只允许一个生效的默认方案，审核时通过事务锁串行校验该约束。
 */
@Data
@Entity
@DynamicInsert
@Table(name = "quality_inspection_plan")
public class QualityInspectionPlanEntity {

    /** 方案版本行主键；检验单引用的是该不可变版本，而非仅引用业务编码。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 跨版本保持一致的方案业务编码，用于聚合同一方案的版本链。 */
    @Column(name = "plan_code")
    private String planCode;

    /** 当前版本的方案展示名称。 */
    @Column(name = "plan_name")
    private String planName;

    /** 可选产品范围；有值时方案仅适用于该产品。 */
    @Column(name = "product_id")
    private Long productId;

    /** 可选客户范围；有值时方案仅适用于该客户。 */
    @Column(name = "customer_id")
    private Long customerId;

    /** 检验类型，决定方案可用于来料、首件、末件、巡检等哪类检验单。 */
    @Column(name = "inspection_type")
    private String inspectionType;

    /** 同一方案编码下单调递增的版本号；并发派生版本时需锁定整条版本链。 */
    @Column(name = "version_no")
    private Integer versionNo;

    /** 生命周期状态：草稿、生效或停用，控制编辑、审核、停用和版本派生权限。 */
    @Column(name = "plan_status")
    private String planStatus;

    /** 业务生效日期；审核时未指定则补为当前日期。 */
    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    /** 是否为当前适用范围的默认方案；审核时需要校验生效默认方案唯一性。 */
    @Column(name = "default_flag")
    private Boolean defaultFlag;

    /** 版本变更背景、适用例外等补充说明。 */
    @Column(name = "remark")
    private String remark;

    /** 创建该版本草稿的操作人标识。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 将草稿审核为生效版本的操作人标识。 */
    @Column(name = "audit_by")
    private Long auditBy;

    /** 审核生效时间，与审核人共同形成状态转换审计证据。 */
    @Column(name = "audit_time")
    private LocalDateTime auditTime;

    /** 数据库生成的版本创建时间，应用层只读。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 数据库维护的最后更新时间，应用层只读。 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记；仅草稿可删除，并通过改写编码避免唯一键占用。 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
