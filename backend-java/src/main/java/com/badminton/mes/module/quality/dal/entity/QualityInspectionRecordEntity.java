package com.badminton.mes.module.quality.dal.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/** 统一质量检验单实体。 */
@Data
@Entity
@DynamicInsert
@Table(name = "quality_inspection_record")
public class QualityInspectionRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inspection_no")
    private String inspectionNo;

    @Column(name = "inspection_type")
    private String inspectionType;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "plan_code_snapshot")
    private String planCodeSnapshot;

    @Column(name = "plan_version_snapshot")
    private Integer planVersionSnapshot;

    @Column(name = "work_order_id")
    private Long workOrderId;

    @Column(name = "production_task_id")
    private Long productionTaskId;

    @Column(name = "source_document_id")
    private Long sourceDocumentId;

    @Column(name = "source_document_no")
    private String sourceDocumentNo;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "production_line_id")
    private Long productionLineId;

    @Column(name = "process_id")
    private Long processId;

    @Column(name = "batch_no")
    private String batchNo;

    @Column(name = "sample_quantity")
    private Integer sampleQuantity;

    @Column(name = "record_status")
    private String recordStatus;

    @Column(name = "conclusion")
    private String conclusion;

    @Column(name = "release_status")
    private String releaseStatus;

    @Column(name = "defect_group_no")
    private String defectGroupNo;

    @Column(name = "defect_quantity")
    private Integer defectQuantity;

    @Column(name = "nonconformance_description")
    private String nonconformanceDescription;

    @Column(name = "disposition")
    private String disposition;

    @Column(name = "inspector_id")
    private Long inspectorId;

    @Column(name = "inspected_at")
    private LocalDateTime inspectedAt;

    @Column(name = "create_by")
    private Long createBy;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    @Column(name = "is_deleted")
    private Boolean deleted;
}
