package com.badminton.mes.module.craft.dal.entity;

import java.time.LocalDateTime;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 质量检验方案引用实体，仅承载工序关联校验所需字段。
 *
 * <p>对应质量模块主表 quality_inspection_plan，按质量模块契约读取
 * plan_status 判定可用性，不负责质量方案写入业务。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
@Entity
@Table(name = "quality_inspection_plan")
public class CraftQualityPlanReferenceEntity {

    /** 质量模块方案生效状态值，只有生效方案可被工序引用 */
    public static final String PLAN_STATUS_EFFECTIVE = "EFFECTIVE";

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 方案状态：DRAFT 草稿 EFFECTIVE 生效 DISABLED 停用 */
    @Column(name = "plan_status")
    private String planStatus;

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
