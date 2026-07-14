package com.badminton.mes.module.quality.dal.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/** 质量检验项目实体。 */
@Data
@Entity
@DynamicInsert
@Table(name = "quality_inspection_item")
public class QualityInspectionItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_code")
    private String itemCode;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "value_type")
    private String valueType;

    @Column(name = "unit")
    private String unit;

    @Column(name = "standard_value")
    private String standardValue;

    @Column(name = "lower_limit")
    private BigDecimal lowerLimit;

    @Column(name = "upper_limit")
    private BigDecimal upperLimit;

    @Column(name = "judgment_method")
    private String judgmentMethod;

    @Column(name = "inspection_method")
    private String inspectionMethod;

    @Column(name = "required_flag")
    private Boolean requiredFlag;

    @Column(name = "enabled_status")
    private Integer enabledStatus;

    @Column(name = "remark")
    private String remark;

    @Column(name = "create_by")
    private Long createBy;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    @Column(name = "is_deleted")
    private Boolean deleted;
}
