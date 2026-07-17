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

/**
 * 质量检验项目实体。
 *
 * <p>项目是可复用的检验规则主数据，通过 {@code categoryId} 归入一个有效分类，并可被多个方案项引用。
 * 数值类型项目需要单位与有序上下限，范围判定仅适用于数值类型，标准值判定则必须提供标准值。
 * 方案审核后，检验单会把项目名称、值类型、单位和最终规则复制为结果快照，因此后续修改本实体不会
 * 篡改已经生成的检验任务与历史判定依据。
 */
@Data
@Entity
@DynamicInsert
@Table(name = "quality_inspection_item")
public class QualityInspectionItemEntity {

    /** 项目数据库主键，由方案项通过 {@code inspectionItemId} 引用。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 项目业务编码；逻辑删除时改写编码，以兼顾历史追溯和原编码复用。 */
    @Column(name = "item_code")
    private String itemCode;

    /** 检验现场与方案维护界面展示的项目名称。 */
    @Column(name = "item_name")
    private String itemName;

    /** 所属分类主键；分类必须存在、未删除且处于启用状态。 */
    @Column(name = "category_id")
    private Long categoryId;

    /** 结果值类型，例如数值或文本；它决定单位、上下限及判定方式的合法组合。 */
    @Column(name = "value_type")
    private String valueType;

    /** 数值结果的计量单位；非数值项目通常不依赖该属性进行判定。 */
    @Column(name = "unit")
    private String unit;

    /** 标准目标值；使用标准值判定法时必须提供，并可被方案项进一步覆盖。 */
    @Column(name = "standard_value")
    private String standardValue;

    /** 数值允许区间下界，与上界共同构成范围判定规则。 */
    @Column(name = "lower_limit")
    private BigDecimal lowerLimit;

    /** 数值允许区间上界，必须不小于下界。 */
    @Column(name = "upper_limit")
    private BigDecimal upperLimit;

    /** 默认判定方式；必须与值类型以及标准值、上下限配置保持一致。 */
    @Column(name = "judgment_method")
    private String judgmentMethod;

    /** 检验操作或仪器方法说明，为执行人员提供采样和测量依据。 */
    @Column(name = "inspection_method")
    private String inspectionMethod;

    /** 项目默认是否必检；方案项可形成自己的必检快照配置。 */
    @Column(name = "required_flag")
    private Boolean requiredFlag;

    /** 启停状态；停用项目不能再加入或更新到检验方案中。 */
    @Column(name = "enabled_status")
    private Integer enabledStatus;

    /** 规则适用条件、设备要求等补充说明。 */
    @Column(name = "remark")
    private String remark;

    /** 创建操作人标识，用于基础规则审计追溯。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 数据库生成的创建时间，应用层只读。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 数据库维护的最后更新时间，应用层只读。 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记；被方案引用的项目不允许删除。 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
