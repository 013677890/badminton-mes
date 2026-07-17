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
 * 生产完工单实体，对应 prod_completion_order。
 *
 * <p>该表由生产侧审核后发布，integration 模块只读取已审核内容，并将发布时的产品、工单和数量
 * 快照提供给 ERP/WMS。实体不建立与生产实体的 JPA 级联，避免集成读取侧的保存操作影响生产主档。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
@Entity
@DynamicInsert
@Table(name = "prod_completion_order")
public class CompletionOrderEntity {

    /** 数据库自增主键，作为完工读取和读取日志关联的稳定标识。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 业务唯一完工单号，也是审核通过发布时的幂等键。 */
    @Column(name = "completion_no")
    private String completionNo;

    /** 现场生产任务主键，用于按生产任务汇总审核通过的完工数量。 */
    @Column(name = "production_task_id")
    private Long productionTaskId;

    /** 生产工单主键，保留完工事实与工单聚合的关联。 */
    @Column(name = "work_order_id")
    private Long workOrderId;

    /** 发布时固化的生产工单号，供外部读取直接展示。 */
    @Column(name = "work_order_no")
    private String workOrderNo;

    /** 发布时对应的产品主键，不建立实体级联关系。 */
    @Column(name = "product_id")
    private Long productId;

    /** 发布时固化的产品业务编码，避免主档修改影响历史完工快照。 */
    @Column(name = "product_code")
    private String productCode;

    /** 发布时固化的产品名称快照。 */
    @Column(name = "product_name")
    private String productName;

    /** 本次完工对应的生产批次号，可为空。 */
    @Column(name = "batch_no")
    private String batchNo;

    /** 本次审核通过的总完工数量。 */
    @Column(name = "completion_quantity")
    private Integer completionQuantity;

    /** 本次完工中的良品数量。 */
    @Column(name = "good_quantity")
    private Integer goodQuantity;

    /** 本次完工中的不良品数量。 */
    @Column(name = "defect_quantity")
    private Integer defectQuantity;

    /** 完工审核状态；外部读取规格固定只允许审核通过状态。 */
    @Column(name = "audit_status")
    private Integer auditStatus;

    /** 审核通过该完工单的系统用户主键。 */
    @Column(name = "audit_by")
    private Long auditBy;

    /** 审核完成时间，也是外部完工查询的时间筛选基准。 */
    @Column(name = "audit_time")
    private LocalDateTime auditTime;

    /** 审核意见或质量补充说明。 */
    @Column(name = "audit_remark")
    private String auditRemark;

    /** 集成快照创建人，通常为审核人或发布链路操作人。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 集成快照最后修改人，供审计追踪。 */
    @Column(name = "update_by")
    private Long updateBy;

    /** 数据库生成的快照创建时间，不由应用插入。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 数据库维护的最后更新时间，不由应用更新。 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记；外部读取和数量聚合均排除已删除快照。 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
