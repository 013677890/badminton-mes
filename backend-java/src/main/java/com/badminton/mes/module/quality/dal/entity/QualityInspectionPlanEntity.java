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

/** 质量检验标准方案实体，每行代表一个不可覆盖的方案版本。 */
@Data
@Entity
@DynamicInsert
@Table(name = "quality_inspection_plan")
public class QualityInspectionPlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_code")
    private String planCode;

    @Column(name = "plan_name")
    private String planName;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "inspection_type")
    private String inspectionType;

    @Column(name = "version_no")
    private Integer versionNo;

    @Column(name = "plan_status")
    private String planStatus;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "default_flag")
    private Boolean defaultFlag;

    @Column(name = "remark")
    private String remark;

    @Column(name = "create_by")
    private Long createBy;

    @Column(name = "audit_by")
    private Long auditBy;

    @Column(name = "audit_time")
    private LocalDateTime auditTime;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    @Column(name = "is_deleted")
    private Boolean deleted;
}
