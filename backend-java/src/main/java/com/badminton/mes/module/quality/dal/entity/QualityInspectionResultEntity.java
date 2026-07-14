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

/** 质量检验项目结果实体。 */
@Data
@Entity
@Table(name = "quality_inspection_result")
public class QualityInspectionResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inspection_record_id")
    private Long inspectionRecordId;

    @Column(name = "inspection_item_id")
    private Long inspectionItemId;

    @Column(name = "item_code_snapshot")
    private String itemCodeSnapshot;

    @Column(name = "item_name_snapshot")
    private String itemNameSnapshot;

    @Column(name = "value_type_snapshot")
    private String valueTypeSnapshot;

    @Column(name = "unit_snapshot")
    private String unitSnapshot;

    @Column(name = "required_flag")
    private Boolean requiredFlag;

    @Column(name = "standard_value_snapshot")
    private String standardValueSnapshot;

    @Column(name = "lower_limit_snapshot")
    private BigDecimal lowerLimitSnapshot;

    @Column(name = "upper_limit_snapshot")
    private BigDecimal upperLimitSnapshot;

    @Column(name = "judgment_method_snapshot")
    private String judgmentMethodSnapshot;

    @Column(name = "measured_value")
    private String measuredValue;

    @Column(name = "judgment_result")
    private String judgmentResult;

    @Column(name = "defect_description")
    private String defectDescription;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
