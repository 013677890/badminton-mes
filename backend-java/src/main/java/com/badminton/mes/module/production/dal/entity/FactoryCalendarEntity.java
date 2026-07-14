package com.badminton.mes.module.production.dal.entity;

import java.time.LocalDate;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 工厂日历实体，对应表 base_factory_calendar。基础资料只读引用。
 *
 * <p>派工校验排产日期是否工作日；无记录的日期按工作日处理(简化约定)。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
@Entity
@Table(name = "base_factory_calendar")
public class FactoryCalendarEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 日历日期 */
    @Column(name = "calendar_date")
    private LocalDate calendarDate;

    /** 适用车间 id */
    @Column(name = "workshop_id")
    private Long workshopId;

    /** 是否工作日：1 是 0 否 */
    @Column(name = "is_workday")
    private Integer workday;

    /** 备注(节假日/调休说明) */
    @Column(name = "remark")
    private String remark;

    /** 逻辑删除标记，映射列 is_deleted */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
