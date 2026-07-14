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
 * 设备故障原理实体，对应表 equip_fault_principle。
 *
 * <p>故障原理是设备报修任务的基础字典，用于规范故障分类、等级和建议处理方案。
 *
 * @author 角色C
 * @date 2026/07/10
 */
@Data
@Entity
@DynamicInsert
@Table(name = "equip_fault_principle")
public class EquipmentFaultPrincipleEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 故障编码，唯一 */
    @Column(name = "fault_code")
    private String faultCode;

    /** 故障名称 */
    @Column(name = "fault_name")
    private String faultName;

    /** 适用设备类别 id，为空表示通用故障 */
    @Column(name = "category_id")
    private Long categoryId;

    /** 故障等级：LOW/MEDIUM/HIGH/CRITICAL */
    @Column(name = "fault_level")
    private String faultLevel;

    /** 故障描述 */
    @Column(name = "fault_description")
    private String faultDescription;

    /** 建议处理方案 */
    @Column(name = "suggested_solution")
    private String suggestedSolution;

    /** 排序号，数字越小越靠前 */
    @Column(name = "sort_order")
    private Integer sortOrder;

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
