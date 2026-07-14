package com.badminton.mes.module.craft.dal.entity;

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
 * 工艺路线主档实体，对应表 craft_route。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "craft_route")
public class CraftRouteEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 路线编码 */
    @Column(name = "routing_code")
    private String routingCode;

    /** 路线名称 */
    @Column(name = "routing_name")
    private String routingName;

    /** 业务版本 */
    @Column(name = "routing_version")
    private String routingVersion;

    /** 上一版本路线主键 */
    @Column(name = "previous_route_id")
    private Long previousRouteId;

    /** 来源：1 本地创建 2 ERP 读取确认 */
    @Column(name = "source_type")
    private Integer sourceType;

    /** 状态：0 草稿 1 生效 2 停用 */
    @Column(name = "routing_status")
    private Integer routingStatus;

    /** 审核人 */
    @Column(name = "audit_by")
    private Long auditBy;

    /** 审核时间 */
    @Column(name = "audit_time")
    private LocalDateTime auditTime;

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
