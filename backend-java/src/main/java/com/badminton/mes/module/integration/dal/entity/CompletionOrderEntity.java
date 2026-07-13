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
 * <p>该表由生产侧写入，integration 模块只读取已审核内容。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
@Entity
@DynamicInsert
@Table(name = "prod_completion_order")
public class CompletionOrderEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 完工单号 */
    @Column(name = "completion_no")
    private String completionNo;

    /** 生产工单主键 */
    @Column(name = "work_order_id")
    private Long workOrderId;

    /** 生产工单号 */
    @Column(name = "work_order_no")
    private String workOrderNo;

    /** 产品主键 */
    @Column(name = "product_id")
    private Long productId;

    /** 产品编码 */
    @Column(name = "product_code")
    private String productCode;

    /** 产品名称 */
    @Column(name = "product_name")
    private String productName;

    /** 产品批次号 */
    @Column(name = "batch_no")
    private String batchNo;

    /** 完工数量 */
    @Column(name = "completion_quantity")
    private Integer completionQuantity;

    /** 良品数量 */
    @Column(name = "good_quantity")
    private Integer goodQuantity;

    /** 不良数量 */
    @Column(name = "defect_quantity")
    private Integer defectQuantity;

    /** 审核状态 */
    @Column(name = "audit_status")
    private Integer auditStatus;

    /** 审核人 */
    @Column(name = "audit_by")
    private Long auditBy;

    /** 审核时间 */
    @Column(name = "audit_time")
    private LocalDateTime auditTime;

    /** 审核意见 */
    @Column(name = "audit_remark")
    private String auditRemark;

    /** 创建人 */
    @Column(name = "create_by")
    private Long createBy;

    /** 最后修改人 */
    @Column(name = "update_by")
    private Long updateBy;

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
