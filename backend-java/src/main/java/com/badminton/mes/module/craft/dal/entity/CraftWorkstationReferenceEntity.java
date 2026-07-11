package com.badminton.mes.module.craft.dal.entity;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 工位只读引用实体，仅承载工艺路线校验所需字段。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
@Entity
@Table(name = "base_workstation")
public class CraftWorkstationReferenceEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 工位编码 */
    @Column(name = "station_code")
    private String stationCode;

    /** 工位名称 */
    @Column(name = "station_name")
    private String stationName;

    /** 状态 */
    @Column(name = "status")
    private Integer status;

    /** 逻辑删除标记 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
