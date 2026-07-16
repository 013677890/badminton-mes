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
 * 设备制造商持久化实体，逐字段映射数据库表 {@code equip_manufacturer}。
 *
 * <p>制造商是设备台账引用的供应来源主数据，集中保存供应商识别信息和售后联系方式。
 * 实体不反向维护设备集合或建立 JPA 级联；{@link DynamicInsert} 允许未赋值列使用数据库默认值。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
@Entity
@DynamicInsert
@Table(name = "equip_manufacturer")
public class EquipmentManufacturerEntity {

    /** 映射主键列 {@code id}；数据库自增生成，供设备台账稳定引用。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 映射 {@code manufacturer_code}；制造商业务唯一编码，用于防重校验和检索。 */
    @Column(name = "manufacturer_code")
    private String manufacturerCode;

    /** 映射 {@code manufacturer_name}；面向业务展示和关键字查询的厂商名称。 */
    @Column(name = "manufacturer_name")
    private String manufacturerName;

    /** 映射 {@code contact_person}；设备采购或售后事务的主要联系人。 */
    @Column(name = "contact_person")
    private String contactPerson;

    /** 映射 {@code contact_phone}；联系人的电话信息，供维修和售后沟通使用。 */
    @Column(name = "contact_phone")
    private String contactPhone;

    /** 映射 {@code contact_email}；制造商业务联系邮箱。 */
    @Column(name = "contact_email")
    private String contactEmail;

    /** 映射 {@code address}；制造商办公、寄送或售后服务地址。 */
    @Column(name = "address")
    private String address;

    /** 映射 {@code website}；制造商官方网站地址，供资料查询使用。 */
    @Column(name = "website")
    private String website;

    /** 映射 {@code remark}；保存合作、服务范围等非结构化补充说明。 */
    @Column(name = "remark")
    private String remark;

    /** 映射 {@code status}；业务启停标记，{@code 1} 启用、{@code 0} 停用。 */
    @Column(name = "status")
    private Integer status;

    /** 映射 {@code create_by}；创建制造商档案的系统用户主键，用于审计。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 映射 {@code create_time}；数据库生成的创建时间，应用不写入也不更新。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 映射 {@code update_time}；档案最后变更时间，插入时可采用数据库默认值。 */
    @Column(name = "update_time", insertable = false)
    private LocalDateTime updateTime;

    /** 映射 {@code is_deleted}；逻辑删除标记，正常查询排除值为 {@code true} 的档案。 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
