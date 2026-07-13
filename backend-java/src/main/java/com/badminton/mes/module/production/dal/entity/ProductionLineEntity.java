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
 * 产线实体，对应表 base_production_line。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "base_production_line")
public class ProductionLineEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 产线编码 */
    @Column(name = "line_code")
    private String lineCode;

    /** 产线名称 */
    @Column(name = "line_name")
    private String lineName;

    /** 所属车间 id */
    @Column(name = "workshop_id")
    private Long workshopId;

    /** 标准日产能(只)，可空(未维护时跳过产能校验) */
    @Column(name = "standard_capacity")
    private Integer standardCapacity;

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
