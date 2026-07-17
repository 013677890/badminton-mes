package com.badminton.mes.module.equipment.dal.entity;

import java.time.LocalDate;
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
 * 设备台账持久化实体，逐字段映射数据库表 {@code equip_ledger}。
 *
 * <p>台账是设备管理模块的核心主数据，点检、保养、报修、计数和 OEE 等业务均通过其主键关联设备。
 * 类别、制造商、车间和产线只保存外部主键，不声明 JPA 级联；{@link DynamicInsert} 保留数据库默认值语义。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
@Entity
@DynamicInsert
@Table(name = "equip_ledger")
public class EquipmentLedgerEntity {

    /** 映射主键列 {@code id}；数据库自增生成，是各设备业务关联台账的稳定标识。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 映射 {@code equipment_code}；设备的业务唯一编码，用于防重校验、检索和现场识别。 */
    @Column(name = "equipment_code")
    private String equipmentCode;

    /** 映射 {@code equipment_name}；面向用户展示和关键字检索的设备名称。 */
    @Column(name = "equipment_name")
    private String equipmentName;

    /** 映射 {@code category_id}；所属设备类别主键，用于分类筛选及业务适配校验。 */
    @Column(name = "category_id")
    private Long categoryId;

    /** 映射 {@code manufacturer_id}；制造商主键，用于追溯供应来源和按厂商筛选。 */
    @Column(name = "manufacturer_id")
    private Long manufacturerId;

    /** 映射 {@code equipment_model}；设备规格型号，参与关键字查询并辅助备件、维修识别。 */
    @Column(name = "equipment_model")
    private String equipmentModel;

    /** 映射 {@code serial_number}；制造商分配的出厂序列号，用于追溯具体实物。 */
    @Column(name = "serial_number")
    private String serialNumber;

    /** 映射 {@code workshop_id}；设备当前所属车间主键，用于生产组织范围筛选。 */
    @Column(name = "workshop_id")
    private Long workshopId;

    /** 映射 {@code production_line_id}；设备当前所属产线主键，用于产线级调度和统计。 */
    @Column(name = "production_line_id")
    private Long productionLineId;

    /** 映射 {@code installation_location}；记录设备在现场的具体安装位置。 */
    @Column(name = "installation_location")
    private String installationLocation;

    /** 映射 {@code purchase_date}；设备采购入账日期，用于资产履历追溯。 */
    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    /** 映射 {@code commissioning_date}；设备正式投入生产的日期。 */
    @Column(name = "commissioning_date")
    private LocalDate commissioningDate;

    /** 映射 {@code equipment_status}；运行状态：IDLE、RUNNING、STOPPED、REPAIRING、MAINTAINING 或 SCRAPPED。 */
    @Column(name = "equipment_status")
    private String equipmentStatus;

    /** 映射 {@code responsible_person}；设备日常管理责任人的姓名或业务标识。 */
    @Column(name = "responsible_person")
    private String responsiblePerson;

    /** 映射 {@code remark}；保存设备特性、使用限制等非结构化补充信息。 */
    @Column(name = "remark")
    private String remark;

    /** 映射 {@code status}；台账业务启停标记，{@code 1} 启用、{@code 0} 停用。 */
    @Column(name = "status")
    private Integer status;

    /** 映射 {@code create_by}；创建设备台账的系统用户主键，供审计追溯。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 映射 {@code create_time}；数据库生成的创建时间，应用不参与插入和更新。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 映射 {@code update_time}；最后变更时间，插入时可采用数据库默认值。 */
    @Column(name = "update_time", insertable = false)
    private LocalDateTime updateTime;

    /** 映射 {@code is_deleted}；逻辑删除标记，常规查询仅返回值为 {@code false} 的台账。 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
