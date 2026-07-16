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
 * 质量检验项目结果实体。
 *
 * <p>每行对应检验单中的一个方案项目。创建检验单时即从方案项和项目主数据复制名称、值类型、单位、必检标记
 * 及完整判定规则，之后只填写实测值、判定结果和缺陷描述。该快照边界使项目或方案的后续修改不会改变历史
 * 检验依据。提交检验单前，服务会检查必检项结果完整性，并依据是否存在失败项约束单据结论与放行状态。
 */
@Data
@Entity
@Table(name = "quality_inspection_result")
public class QualityInspectionResultEntity {

    /** 结果行主键，也是批量保存结果时客户端回传的精确定位标识。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属检验单主键；结果只能在该检验单仍为草稿时更新。 */
    @Column(name = "inspection_record_id")
    private Long inspectionRecordId;

    /** 原始检验项目主键，用于追溯规则来源，不作为历史展示的唯一依据。 */
    @Column(name = "inspection_item_id")
    private Long inspectionItemId;

    /** 创建检验单时的项目编码快照。 */
    @Column(name = "item_code_snapshot")
    private String itemCodeSnapshot;

    /** 创建检验单时的项目名称快照。 */
    @Column(name = "item_name_snapshot")
    private String itemNameSnapshot;

    /** 结果值类型快照，决定实测值的业务解释方式。 */
    @Column(name = "value_type_snapshot")
    private String valueTypeSnapshot;

    /** 计量单位快照，保证历史实测值具备完整量纲。 */
    @Column(name = "unit_snapshot")
    private String unitSnapshot;

    /** 必检标记快照；为真时缺少实测值或判定结果将阻止提交。 */
    @Column(name = "required_flag")
    private Boolean requiredFlag;

    /** 标准目标值快照，来源于方案项最终采用的规则。 */
    @Column(name = "standard_value_snapshot")
    private String standardValueSnapshot;

    /** 允许区间下界快照。 */
    @Column(name = "lower_limit_snapshot")
    private BigDecimal lowerLimitSnapshot;

    /** 允许区间上界快照。 */
    @Column(name = "upper_limit_snapshot")
    private BigDecimal upperLimitSnapshot;

    /** 判定方式快照，记录本次检验应采用范围、标准值或人工判定等规则。 */
    @Column(name = "judgment_method_snapshot")
    private String judgmentMethodSnapshot;

    /** 检验员录入的实测值；采用字符串承载不同值类型的原始表达。 */
    @Column(name = "measured_value")
    private String measuredValue;

    /** 项目级判定结果；失败结果会参与单据结论一致性校验。 */
    @Column(name = "judgment_result")
    private String judgmentResult;

    /** 失败项目的具体缺陷现象，用于支撑不符合描述和后续处置。 */
    @Column(name = "defect_description")
    private String defectDescription;

    /** 沿用方案项顺序，确保录入界面和历史报告稳定展示。 */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /** 数据库生成的结果快照创建时间，应用层只读。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 数据库维护的结果最后更新时间，应用层只读。 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
