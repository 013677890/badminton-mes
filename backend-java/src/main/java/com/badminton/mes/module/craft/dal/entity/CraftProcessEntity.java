package com.badminton.mes.module.craft.dal.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * 工序档案实体，对应表 craft_process。
 *
 * <p>关键、质检、扫码和计件标志是面向现场、质量与工资模块的稳定规则契约。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "craft_process")
public class CraftProcessEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 工序编码 */
    @Column(name = "process_code")
    private String processCode;

    /** 工序名称 */
    @Column(name = "process_name")
    private String processName;

    /** 工序类型编码 */
    @Column(name = "process_type")
    private String processType;

    /** 标准工时，单位秒 */
    @Column(name = "standard_time_seconds")
    private Integer standardTimeSeconds;

    /** 是否关键工序 */
    @Column(name = "is_key_process")
    private Boolean keyProcess;

    /** 是否需要质检 */
    @Column(name = "is_quality_required")
    private Boolean qualityRequired;

    /** 是否需要扫码 */
    @Column(name = "is_scan_required")
    private Boolean scanRequired;

    /** 是否参与计件 */
    @Column(name = "is_piece_rate_enabled")
    private Boolean pieceRateEnabled;

    /** 适用设备类别 id */
    @Column(name = "equipment_category_id")
    private Long equipmentCategoryId;

    /** 检验方案 id */
    @Column(name = "quality_plan_id")
    private Long qualityPlanId;

    /** 备注 */
    @Column(name = "remark")
    private String remark;

    /** 状态：1 启用 0 停用 */
    @Column(name = "status")
    private Integer status;

    /** 乐观锁版本 */
    @Version
    @Column(name = "version")
    private Integer version;

    /** 创建人 */
    @Column(name = "create_by")
    private Long createBy;

    /** 最后修改人 */
    @Column(name = "update_by")
    private Long updateBy;

    /** 创建时间 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
