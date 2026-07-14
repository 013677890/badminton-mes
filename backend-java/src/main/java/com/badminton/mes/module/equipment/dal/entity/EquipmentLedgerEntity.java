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
 * 设备台账实体，对应表 equip_ledger。
 *
 * <p>台账是设备管理模块的核心主数据，点检、保养、报修、计数、OEE 等后续业务均以设备台账为基础。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
@Entity
@DynamicInsert
@Table(name = "equip_ledger")
public class EquipmentLedgerEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 设备编码，唯一 */
    @Column(name = "equipment_code")
    private String equipmentCode;

    /** 设备名称 */
    @Column(name = "equipment_name")
    private String equipmentName;

    /** 设备类别 id */
    @Column(name = "category_id")
    private Long categoryId;

    /** 设备制造商 id */
    @Column(name = "manufacturer_id")
    private Long manufacturerId;

    /** 规格型号 */
    @Column(name = "equipment_model")
    private String equipmentModel;

    /** 出厂编号 */
    @Column(name = "serial_number")
    private String serialNumber;

    /** 所属车间 id */
    @Column(name = "workshop_id")
    private Long workshopId;

    /** 所属产线 id */
    @Column(name = "production_line_id")
    private Long productionLineId;

    /** 安装位置 */
    @Column(name = "installation_location")
    private String installationLocation;

    /** 采购日期 */
    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    /** 启用日期 */
    @Column(name = "commissioning_date")
    private LocalDate commissioningDate;

    /** 设备状态：IDLE/RUNNING/STOPPED/REPAIRING/MAINTAINING/SCRAPPED */
    @Column(name = "equipment_status")
    private String equipmentStatus;

    /** 负责人 */
    @Column(name = "responsible_person")
    private String responsiblePerson;

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
