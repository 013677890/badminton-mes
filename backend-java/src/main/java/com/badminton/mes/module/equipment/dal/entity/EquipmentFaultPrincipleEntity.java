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
 * 设备故障原理持久化实体，逐字段映射数据库表 {@code equip_fault_principle}。
 *
 * <p>故障原理是报修任务引用的标准字典，用于统一故障分类、严重等级和建议处理方案。
 * 设备类别仅以主键关联，不建立 JPA 级联；{@link DynamicInsert} 允许数据库为未赋值列填充默认值。
 *
 * @author 角色C
 * @date 2026/07/10
 */
@Data
@Entity
@DynamicInsert
@Table(name = "equip_fault_principle")
public class EquipmentFaultPrincipleEntity {

    /** 映射主键列 {@code id}；数据库自增生成，作为报修单引用故障原理的稳定标识。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 映射 {@code fault_code}；故障原理的业务唯一编码，用于防重校验和检索。 */
    @Column(name = "fault_code")
    private String faultCode;

    /** 映射 {@code fault_name}；面向报修人员展示的标准故障名称。 */
    @Column(name = "fault_name")
    private String faultName;

    /** 映射 {@code category_id}；限定适用设备类别的主键，为空表示可用于任意类别。 */
    @Column(name = "category_id")
    private Long categoryId;

    /** 映射 {@code fault_level}；严重等级枚举：LOW、MEDIUM、HIGH 或 CRITICAL。 */
    @Column(name = "fault_level")
    private String faultLevel;

    /** 映射 {@code fault_description}；保存标准故障现象、判断依据等说明。 */
    @Column(name = "fault_description")
    private String faultDescription;

    /** 映射 {@code suggested_solution}；提供给维修人员的排查或处置建议。 */
    @Column(name = "suggested_solution")
    private String suggestedSolution;

    /** 映射 {@code sort_order}；故障原理在列表中的展示顺序，数值越小越靠前。 */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /** 映射 {@code remark}；保存适用限制等非结构化补充信息。 */
    @Column(name = "remark")
    private String remark;

    /** 映射 {@code status}；业务启停标记，{@code 1} 启用、{@code 0} 停用。 */
    @Column(name = "status")
    private Integer status;

    /** 映射 {@code create_by}；创建该字典项的系统用户主键，用于审计。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 映射 {@code create_time}；由数据库在插入时生成，应用不写入也不更新。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 映射 {@code update_time}；记录最后变更时间，插入时可使用数据库默认值。 */
    @Column(name = "update_time", insertable = false)
    private LocalDateTime updateTime;

    /** 映射 {@code is_deleted}；逻辑删除标记，常规业务查询必须过滤已删除数据。 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
