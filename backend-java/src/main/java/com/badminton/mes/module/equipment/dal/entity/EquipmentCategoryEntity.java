package com.badminton.mes.module.equipment.dal.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 设备类别实体，对应表 equip_category。
 *
 * <p>实体只承载数据库状态，不建立 JPA 级联关系，不调用 Service 或 Redis。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
@Entity
@DynamicInsert
@Table(name = "equip_category")
public class EquipmentCategoryEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 类别编码，唯一 */
    @Column(name = "category_code")
    private String categoryCode;

    /** 类别名称 */
    @Column(name = "category_name")
    private String categoryName;

    /** 父级类别 id，顶级为 null */
    @Column(name = "parent_id")
    private Long parentId;

    /** 排序号，数字越小越靠前 */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /** 备注说明 */
    @Column(name = "remark")
    private String remark;

    /** 状态：1 启用 0 停用 */
    @Column(name = "status", columnDefinition = "tinyint")
    private Integer status;

    /** 创建人用户 id */
    @Column(name = "create_by")
    private Long createBy;

    /** 创建时间 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time", insertable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记，映射列 is_deleted */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
