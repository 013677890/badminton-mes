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
import lombok.Data;

/** 计件工资结算明细实体。 */
@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "wage_settlement_detail")
public class WageSettlementDetailEntity {
    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** 结算主键 */
    @Column(name = "settlement_id")
    private Long settlementId;
    /** 报工快照主键 */
    @Column(name = "work_record_id")
    private Long workRecordId;
    /** 规则主键 */
    @Column(name = "rule_id")
    private Long ruleId;
    /** 员工主键 */
    @Column(name = "employee_id")
    private Long employeeId;
    /** 作业日期 */
    @Column(name = "work_date")
    private LocalDate workDate;
    /** 工单主键 */
    @Column(name = "work_order_id")
    private Long workOrderId;
    /** 工序主键 */
    @Column(name = "process_id")
    private Long processId;
    /** 产品主键 */
    @Column(name = "product_id")
    private Long productId;
    /** 合格数量快照 */
    @Column(name = "qualified_quantity")
    private BigDecimal qualifiedQuantity;
    /** 不良数量快照 */
    @Column(name = "defect_quantity")
    private BigDecimal defectQuantity;
    /** 单价快照，万分之一元 */
    @Column(name = "unit_price_basis")
    private Long unitPriceBasis;
    /** 不良扣减率快照 */
    @Column(name = "defect_deduction_rate")
    private Integer defectDeductionRate;
    /** 系统计算金额 */
    @Column(name = "calculated_amount_basis")
    private Long calculatedAmountBasis;
    /** 人工调整金额 */
    @Column(name = "adjusted_amount_basis")
    private Long adjustedAmountBasis;
    /** 最终金额 */
    @Column(name = "final_amount_basis")
    private Long finalAmountBasis;
    /** 是否占用来源报工 */
    @Column(name = "is_active")
    private Boolean active;
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
