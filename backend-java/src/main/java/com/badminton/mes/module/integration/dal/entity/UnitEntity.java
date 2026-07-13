package com.badminton.mes.module.integration.dal.entity;

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
 * 计量单位实体，对应 base_unit。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "base_unit")
public class UnitEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 单位编码 */
    @Column(name = "unit_code")
    private String unitCode;

    /** 单位名称 */
    @Column(name = "unit_name")
    private String unitName;

    /** 数量小数精度 */
    @Column(name = "decimal_precision")
    private Integer decimalPrecision;

    /** 状态：1 启用 0 停用 */
    @Column(name = "status")
    private Integer status;

    /** 乐观锁版本 */
    @Version
    @Column(name = "version")
    private Integer version;

    /** 创建人 */
    @Column(name = "create_by")
    private Long createBy;

    /** 最后修改人 */
    @Column(name = "update_by")
    private Long updateBy;

    /** 创建时间 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
