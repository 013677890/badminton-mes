package com.badminton.mes.module.production.dal.entity;

import java.time.LocalTime;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 班次实体，对应表 base_shift。基础资料只读引用。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
@Entity
@Table(name = "base_shift")
public class ShiftEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 班次编码 */
    @Column(name = "shift_code")
    private String shiftCode;

    /** 班次名称(白班/夜班) */
    @Column(name = "shift_name")
    private String shiftName;

    /** 班次开始时间 */
    @Column(name = "start_time")
    private LocalTime startTime;

    /** 班次结束时间 */
    @Column(name = "end_time")
    private LocalTime endTime;

    /** 状态：1 启用 0 停用 */
    @Column(name = "status")
    private Integer status;

    /** 逻辑删除标记，映射列 is_deleted */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
