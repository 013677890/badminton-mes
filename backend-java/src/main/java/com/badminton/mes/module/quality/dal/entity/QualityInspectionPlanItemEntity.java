package com.badminton.mes.module.quality.dal.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/** 检验方案明细引用实体。 */
@Data
@Entity
@Table(name = "quality_inspection_plan_item")
public class QualityInspectionPlanItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "inspection_item_id")
    private Long inspectionItemId;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "sample_quantity")
    private Integer sampleQuantity;

    @Column(name = "required_flag")
    private Boolean requiredFlag;

    @Column(name = "standard_value")
    private String standardValue;

    @Column(name = "lower_limit")
    private BigDecimal lowerLimit;

    @Column(name = "upper_limit")
    private BigDecimal upperLimit;

    @Column(name = "judgment_method")
    private String judgmentMethod;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;
}
