package com.badminton.mes.module.production.dal.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * 物料实体，对应表 base_material。
 *
 * <p>基础资料模块尚未建设，本实体仅映射工单物料需求生成时需要读取的列。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "base_material")
public class MaterialEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 物料编码 */
    @Column(name = "material_code")
    private String materialCode;

    /** 物料名称 */
    @Column(name = "material_name")
    private String materialName;

    /** 规格型号 */
    @Column(name = "spec")
    private String spec;

    /** 物料类型：1 球头 2 羽片 3 胶水 4 线圈 5 包装材料 9 其他 */
    @Column(name = "material_type")
    private Integer materialType;

    /** 计量单位 id */
    @Column(name = "unit_id")
    private Long unitId;

    /** 是否关键物料 */
    @Column(name = "is_key_material")
    private Boolean keyMaterial;

    /** 状态：1 启用 0 停用 */
    @Column(name = "status")
    private Integer status;

    /** 创建人 */
    @Column(name = "create_by")
    private Long createBy;

    /** 最后修改人 */
    @Column(name = "update_by")
    private Long updateBy;

    /** 乐观锁版本 */
    @Version
    @Column(name = "version")
    private Integer version;

    /** 创建时间 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记，映射列 is_deleted */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
