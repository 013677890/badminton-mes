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

/**
 * 检验方案明细引用实体。
 *
 * <p>方案项连接一个具体方案版本和一个检验项目，同时保存该版本实际采用的采样数量、必检标记与判定规则。
 * 创建方案项时，未显式覆盖的规则从项目主数据补齐；派生新版本时按当前方案项原样复制。因此它既是引用关系，
 * 也是方案级规则快照，避免项目主数据后续调整反向改变已审核方案的业务含义。
 */
@Data
@Entity
@Table(name = "quality_inspection_plan_item")
public class QualityInspectionPlanItemEntity {

    /** 方案项主键。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属方案版本主键；同一版本内项目不得重复。 */
    @Column(name = "plan_id")
    private Long planId;

    /** 被引用的检验项目主键，用于补充名称、类型与默认规则。 */
    @Column(name = "inspection_item_id")
    private Long inspectionItemId;

    /** 方案内执行及展示顺序；生成检验结果快照时沿用该顺序。 */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /** 该项目在本方案中的采样数量要求。 */
    @Column(name = "sample_quantity")
    private Integer sampleQuantity;

    /** 本方案版本是否要求该项目必须填写完整结果。 */
    @Column(name = "required_flag")
    private Boolean requiredFlag;

    /** 本方案采用的标准值快照，可覆盖项目主数据默认值。 */
    @Column(name = "standard_value")
    private String standardValue;

    /** 本方案采用的允许区间下界快照。 */
    @Column(name = "lower_limit")
    private BigDecimal lowerLimit;

    /** 本方案采用的允许区间上界快照。 */
    @Column(name = "upper_limit")
    private BigDecimal upperLimit;

    /** 本方案采用的判定方式快照，必须与值类型和阈值组合相容。 */
    @Column(name = "judgment_method")
    private String judgmentMethod;

    /** 数据库生成的方案项创建时间，应用层只读。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;
}
