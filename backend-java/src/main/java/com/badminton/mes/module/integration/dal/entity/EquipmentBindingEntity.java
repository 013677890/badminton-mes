package com.badminton.mes.module.integration.dal.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 设备报工绑定配置实体。
 *
 * <p>以设备编码为外部采集入口，关联产线、可选工序和默认报工人，并保存自动报工开关及累计
 * 增量上限。实体只保留关联主键，不建立级联关系；启用状态由设备计数命令在读取时再次判断。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "integration_equipment_binding")
public class EquipmentBindingEntity {

    /** 绑定配置自增主键。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 规范化设备业务编码，作为一台设备的绑定查询键。 */
    @Column(name = "equipment_code")
    private String equipmentCode;

    /** 设备允许上报的产线主键。 */
    @Column(name = "line_id")
    private Long lineId;

    /** 可选固定工序主键，为空表示允许该产线下多个工序。 */
    @Column(name = "process_id")
    private Long processId;

    /** 自动报工使用的默认员工主键。 */
    @Column(name = "default_employee_id")
    private Long defaultEmployeeId;

    /** 是否在有效设备计数后自动创建现场报工。 */
    @Column(name = "is_auto_report")
    private Boolean autoReport;

    /** 单次允许的最大累计增量，超过时进入计数异常池。 */
    @Column(name = "max_increment")
    private Long maxIncrement;

    /** 绑定启停状态；停用配置按未绑定处理。 */
    @Column(name = "status")
    private Integer status;

    /** 创建绑定的系统用户主键。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 最后修改绑定的系统用户主键。 */
    @Column(name = "update_by")
    private Long updateBy;

    /** 数据库生成的创建时间。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 数据库维护的最后更新时间。 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记，正常绑定查询只读取 false。 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
