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

/** 质量检验项目分类实体。 */
@Data
@Entity
@DynamicInsert
@Table(name = "quality_inspection_category")
public class QualityInspectionCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_code")
    private String categoryCode;

    @Column(name = "category_name")
    private String categoryName;

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
