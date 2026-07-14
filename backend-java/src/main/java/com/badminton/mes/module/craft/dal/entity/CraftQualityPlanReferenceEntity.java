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
 * <p>对应质量模块主表 quality_inspection_plan，不负责质量方案写入业务。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
@Entity
@Table(name = "quality_inspection_plan")
public class CraftQualityPlanReferenceEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 启停状态 */
    @Column(name = "status")
    private Integer status;

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
