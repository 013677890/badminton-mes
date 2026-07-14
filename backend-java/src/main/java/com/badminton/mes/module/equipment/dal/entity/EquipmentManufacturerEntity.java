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
 * 设备制造商实体，对应表 equip_manufacturer。
 *
 * <p>实体只承载数据库状态，不建立 JPA 级联关系，不调用 Service 或 Redis。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
@Entity
@DynamicInsert
@Table(name = "equip_manufacturer")
public class EquipmentManufacturerEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 制造商编码，唯一 */
    @Column(name = "manufacturer_code")
    private String manufacturerCode;

    /** 制造商名称 */
    @Column(name = "manufacturer_name")
    private String manufacturerName;

    /** 联系人 */
    @Column(name = "contact_person")
    private String contactPerson;

    /** 联系电话 */
    @Column(name = "contact_phone")
    private String contactPhone;

    /** 联系邮箱 */
    @Column(name = "contact_email")
    private String contactEmail;

    /** 地址 */
    @Column(name = "address")
    private String address;

    /** 官网 */
    @Column(name = "website")
    private String website;

    /** 备注说明 */
    @Column(name = "remark")
    private String remark;

    /** 状态：1 启用 0 停用 */
    @Column(name = "status")
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
