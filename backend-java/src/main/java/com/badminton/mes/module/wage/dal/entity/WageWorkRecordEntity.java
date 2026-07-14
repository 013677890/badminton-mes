package com.badminton.mes.module.wage.dal.entity;

import java.math.BigDecimal;
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

/** 已审核报工的计件快照实体。 */
@Data
@Entity
@DynamicInsert
@Table(name = "wage_work_record")
public class WageWorkRecordEntity {
    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** 来源报工主键 */
    @Column(name = "source_report_id")
    private Long sourceReportId;
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
    /** 合格数量 */
    @Column(name = "qualified_quantity")
    private BigDecimal qualifiedQuantity;
    /** 不良数量 */
    @Column(name = "defect_quantity")
    private BigDecimal defectQuantity;
    /** 来源审核时间 */
    @Column(name = "source_audit_time")
    private LocalDateTime sourceAuditTime;
    /** 导入人 */
    @Column(name = "create_by")
    private Long createBy;
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
