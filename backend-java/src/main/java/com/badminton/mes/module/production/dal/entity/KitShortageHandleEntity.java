package com.badminton.mes.module.production.dal.entity;

import java.time.LocalDate;
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
 * 欠料处理记录实体，对应表 prod_kit_shortage_handle。
 *
 * <p>登记催采购/调拨/代用料/调整排产等处置措施与责任人，
 * 欠料看板取物料未解决记录的预计到料日期。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
@Entity
@DynamicInsert
@Table(name = "prod_kit_shortage_handle")
public class KitShortageHandleEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 生产工单 id */
    @Column(name = "work_order_id")
    private Long workOrderId;

    /** 欠料物料 id */
    @Column(name = "material_id")
    private Long materialId;

    /** 处理方式：1 催采购 2 调拨 3 代用料 4 调整排产 */
    @Column(name = "handle_type")
    private Integer handleType;

    /** 责任人(sys_user.id) */
    @Column(name = "handler_id")
    private Long handlerId;

    /** 预计到料日期 */
    @Column(name = "expected_arrival_date")
    private LocalDate expectedArrivalDate;

    /** 处理说明 */
    @Column(name = "handle_remark")
    private String handleRemark;

    /** 处理状态：0 处理中 1 已解决 */
    @Column(name = "handle_status")
    private Integer handleStatus;

    /** 创建人 */
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
