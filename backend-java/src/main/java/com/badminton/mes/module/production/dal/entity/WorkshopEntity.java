package com.badminton.mes.module.production.dal.entity;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 车间实体，对应表 base_workshop。
 *
 * <p>基础资料模块尚未建设，本实体仅映射生产订单模块创建工单时需要读取的列。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
@Data
@Entity
@Table(name = "base_workshop")
public class WorkshopEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 车间编码 */
    @Column(name = "workshop_code")
    private String workshopCode;

    /** 车间名称 */
    @Column(name = "workshop_name")
    private String workshopName;

    /** 状态：1 启用 0 停用 */
    @Column(name = "status", columnDefinition = "tinyint unsigned")
    private Integer status;

    /** 逻辑删除标记，映射列 is_deleted */
    @Column(name = "is_deleted", columnDefinition = "tinyint unsigned")
    private Boolean deleted;
}
