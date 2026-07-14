package com.badminton.mes.module.wage.dal.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

/** 计件工资结算批次实体。 */
@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "wage_settlement")
public class WageSettlementEntity {
    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** 结算批次号 */
    @Column(name = "settlement_no")
    private String settlementNo;
    /** 结算开始日期 */
    @Column(name = "period_start")
    private LocalDate periodStart;
    /** 结算结束日期 */
    @Column(name = "period_end")
    private LocalDate periodEnd;
    /** 员工范围 JSON */
    @Column(name = "employee_scope", columnDefinition = "json")
    private String employeeScope;
    /** 结算状态 */
    @Column(name = "settlement_status")
    private Integer settlementStatus;
    /** 合格数量合计 */
    @Column(name = "total_qualified_quantity")
    private BigDecimal totalQualifiedQuantity;
    /** 不良数量合计 */
    @Column(name = "total_defect_quantity")
    private BigDecimal totalDefectQuantity;
    /** 最终金额合计，万分之一元 */
    @Column(name = "total_amount_basis")
    private Long totalAmountBasis;
    /** 乐观锁版本 */
    @Version
    @Column(name = "version")
    private Integer version;
    /** 提交人 */
    @Column(name = "submit_by")
    private Long submitBy;
    /** 提交时间 */
    @Column(name = "submit_time")
    private LocalDateTime submitTime;
    /** 审核人 */
    @Column(name = "audit_by")
    private Long auditBy;
    /** 审核时间 */
    @Column(name = "audit_time")
    private LocalDateTime auditTime;
    /** 审核意见 */
    @Column(name = "audit_reason")
    private String auditReason;
    /** 创建人 */
    @Column(name = "create_by")
    private Long createBy;
    /** 修改人 */
    @Column(name = "update_by")
    private Long updateBy;
    /** 创建时间 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;
    /** 更新时间 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
    /** 逻辑删除 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
