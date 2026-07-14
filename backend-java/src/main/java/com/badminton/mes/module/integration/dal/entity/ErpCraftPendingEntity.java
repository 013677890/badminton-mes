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

/**
 * ERP 工艺待确认数据实体，对应表 erp_craft_pending。
 *
 * <p>ERP 读取的工艺路线数据暂存于此表，校验通过为待确认、校验失败为异常。
 * 工艺工程师确认后调用 CraftRouteService.createRoute 生成草稿。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "erp_craft_pending")
public class ErpCraftPendingEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 来源系统 */
    @Column(name = "source_system")
    private String sourceSystem;

    /** ERP 工艺路线编码 */
    @Column(name = "erp_routing_code")
    private String erpRoutingCode;

    /** ERP 工艺路线名称 */
    @Column(name = "erp_routing_name")
    private String erpRoutingName;

    /** ERP 工艺路线版本 */
    @Column(name = "erp_routing_version")
    private String erpRoutingVersion;

    /** 产品编码 */
    @Column(name = "product_code")
    private String productCode;

    /** 状态：0 待确认 1 已确认 2 异常 */
    @Column(name = "status")
    private Integer status;

    /** 工序步骤 JSON */
    @Column(name = "process_steps", columnDefinition = "json")
    private String processSteps;

    /** 确认后生成的工艺路线主键 */
    @Column(name = "confirmed_route_id")
    private Long confirmedRouteId;

    /** 确认人 */
    @Column(name = "confirmed_by")
    private Long confirmedBy;

    /** 确认时间 */
    @Column(name = "confirm_time")
    private LocalDateTime confirmTime;

    /** 异常错误码 */
    @Column(name = "error_code")
    private String errorCode;

    /** 异常原因 */
    @Column(name = "error_message")
    private String errorMessage;

    /** 创建人 */
    @Column(name = "create_by")
    private Long createBy;

    /** 创建时间 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
