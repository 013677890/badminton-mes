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
 * 设备类别持久化实体，逐字段映射数据库表 {@code equip_category}。
 *
 * <p>该表保存可形成父子树的设备分类主数据，供设备台账、故障原理、工序和工艺路线引用。
 * 实体只保存关联对象的主键，不建立 JPA 级联关系；{@link DynamicInsert} 使未赋值列沿用数据库默认值。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
@Entity
@DynamicInsert
@Table(name = "equip_category")
public class EquipmentCategoryEntity {

    /** 映射主键列 {@code id}；由数据库自增生成，作为类别及其下游引用的稳定标识。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 映射 {@code category_code}；面向业务的类别唯一编码，用于录入校验和展示检索。 */
    @Column(name = "category_code")
    private String categoryCode;

    /** 映射 {@code category_name}；供用户识别和关键字查询的类别名称。 */
    @Column(name = "category_name")
    private String categoryName;

    /** 映射 {@code parent_id}；保存父类别主键以组织分类树，顶级类别取 {@code null}。 */
    @Column(name = "parent_id")
    private Long parentId;

    /** 映射 {@code sort_order}；同层类别的展示顺序，数值越小越靠前。 */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /** 映射 {@code remark}；保存类别用途、适用范围等非结构化补充信息。 */
    @Column(name = "remark")
    private String remark;

    /** 映射 {@code status}；业务启停标记，{@code 1} 表示启用，{@code 0} 表示停用。 */
    @Column(name = "status")
    private Integer status;

    /** 映射 {@code create_by}；记录创建该类别的系统用户主键，供审计追溯。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 映射 {@code create_time}；由数据库在插入时生成，应用不参与写入和更新。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 映射 {@code update_time}；保存最后变更时间，插入时可采用数据库默认值。 */
    @Column(name = "update_time", insertable = false)
    private LocalDateTime updateTime;

    /** 映射 {@code is_deleted}；逻辑删除标记，正常查询仅保留值为 {@code false} 的类别。 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
