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

/**
 * 统一质量检验单实体。
 *
 * <p>检验单统一承载不同来源的质量任务：生产类检验必须关联工单，并从工单校验产品、客户和批次；
 * 生产线由创建请求指定。其他类型可关联相应来源单据。检验类型、来源范围与方案的产品、客户及检验类型
 * 范围必须一致。创建时保存方案编码和版本快照，并同步生成项目结果快照；提交前必须完成所有必检结果。
 * 提交结论会驱动放行状态：合格或让步接收允许放行，其他不合格处置进入阻断，以免未经质量确认的对象
 * 流入后续业务。
 */
@Data
@Entity
@DynamicInsert
@Table(name = "quality_inspection_record")
public class QualityInspectionRecordEntity {

    /** 检验单数据库主键，也是各项目结果的聚合根标识。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 全局检验单号，用于现场流转、查询和外部单据关联。 */
    @Column(name = "inspection_no")
    private String inspectionNo;

    /** 检验类型；决定来源字段要求，并必须与所选方案类型一致。 */
    @Column(name = "inspection_type")
    private String inspectionType;

    /** 创建检验单时采用的具体方案版本主键。 */
    @Column(name = "plan_id")
    private Long planId;

    /** 方案业务编码快照，确保方案后续逻辑删除或展示变化后仍可追溯。 */
    @Column(name = "plan_code_snapshot")
    private String planCodeSnapshot;

    /** 方案版本号快照，与方案编码共同标识当时采用的标准。 */
    @Column(name = "plan_version_snapshot")
    private Integer planVersionSnapshot;

    /** 生产类检验关联的工单主键，用于校验真实生产范围。 */
    @Column(name = "work_order_id")
    private Long workOrderId;

    /** 非工单或扩展来源单据的主键。 */
    @Column(name = "source_document_id")
    private Long sourceDocumentId;

    /** 来源单据号快照，便于不依赖来源模块即可展示和追溯。 */
    @Column(name = "source_document_no")
    private String sourceDocumentNo;

    /** 被检产品主键；需要与方案产品范围及生产工单保持一致。 */
    @Column(name = "product_id")
    private Long productId;

    /** 客户主键；用于匹配客户专用检验方案的适用范围。 */
    @Column(name = "customer_id")
    private Long customerId;

    /** 生产线主键；由创建请求提供，用于描述本次生产类检验的现场范围。 */
    @Column(name = "production_line_id")
    private Long productionLineId;

    /** 被检批次号，用于将结论约束到具体生产或来料批次。 */
    @Column(name = "batch_no")
    private String batchNo;

    /** 检验单总体抽样数量，与各方案项的项目级采样要求配合使用。 */
    @Column(name = "sample_quantity")
    private Integer sampleQuantity;

    /** 单据状态：草稿允许保存结果，提交后禁止继续修改。 */
    @Column(name = "record_status")
    private String recordStatus;

    /** 质量结论；必须与项目级失败结果及不合格信息保持逻辑一致。 */
    @Column(name = "conclusion")
    private String conclusion;

    /** 对后续业务的放行状态；创建时待处理，提交后按结论映射为已发布或阻断。 */
    @Column(name = "release_status")
    private String releaseStatus;

    /** 存在失败项目时必填的不符合事实说明。 */
    @Column(name = "nonconformance_description")
    private String nonconformanceDescription;

    /** 非合格结论对应的处置意见，例如返工、报废或让步评审。 */
    @Column(name = "disposition")
    private String disposition;

    /** 最终提交检验单的检验员标识。 */
    @Column(name = "inspector_id")
    private Long inspectorId;

    /** 检验完成并提交的时间。 */
    @Column(name = "inspected_at")
    private LocalDateTime inspectedAt;

    /** 创建检验任务的操作人标识。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 数据库生成的创建时间，应用层只读。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 数据库维护的最后更新时间，应用层只读。 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记；常规查询、更新锁和缓存加载只处理未删除单据。 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
