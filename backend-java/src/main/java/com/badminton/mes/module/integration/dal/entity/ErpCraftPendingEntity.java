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
 * 工艺工程师确认后调用 CraftRouteService.createRoute 生成草稿。步骤以 JSON 快照保存，确认时
 * 重新解析当前 MES 产品和工序主键，避免暂存记录直接持有跨聚合实体关系。
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

    /** 待确认记录自增主键。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ERP 来源系统，参与路线版本幂等键。 */
    @Column(name = "source_system")
    private String sourceSystem;

    /** ERP 工艺路线业务编码。 */
    @Column(name = "erp_routing_code")
    private String erpRoutingCode;

    /** ERP 工艺路线名称快照。 */
    @Column(name = "erp_routing_name")
    private String erpRoutingName;

    /** ERP 工艺路线版本，和编码共同定位一版来源工艺。 */
    @Column(name = "erp_routing_version")
    private String erpRoutingVersion;

    /** 工艺适用产品编码，确认时解析为 MES 产品主键。 */
    @Column(name = "product_code")
    private String productCode;

    /** 处理状态：待确认、已确认或异常/驳回等业务状态。 */
    @Column(name = "status")
    private Integer status;

    /** 排序和校验后的 ERP 工序步骤 JSON 快照。 */
    @Column(name = "process_steps", columnDefinition = "json")
    private String processSteps;

    /** 确认后生成的 MES 工艺路线主键。 */
    @Column(name = "confirmed_route_id")
    private Long confirmedRouteId;

    /** 将暂存工艺确认生成草稿的系统用户主键。 */
    @Column(name = "confirmed_by")
    private Long confirmedBy;

    /** 工艺确认完成时间。 */
    @Column(name = "confirm_time")
    private LocalDateTime confirmTime;

    /** 校验或人工处理产生的稳定业务错误码。 */
    @Column(name = "error_code")
    private String errorCode;

    /** 供人工查看的校验失败或驳回原因。 */
    @Column(name = "error_message")
    private String errorMessage;

    /** 首次同步该 ERP 工艺的系统用户主键。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 暂存记录创建时间，由数据库维护。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 暂存记录最后更新时间，由数据库维护。 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
