package com.badminton.mes.module.craft.dal.entity;

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
 * 工艺路线变更日志实体，对应表 craft_route_change_log。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
@Entity
@DynamicInsert
@Table(name = "craft_route_change_log")
public class CraftRouteChangeLogEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 工艺路线主键 */
    @Column(name = "route_id")
    private Long routeId;

    /** 变更类型 */
    @Column(name = "change_type")
    private Integer changeType;

    /** 变更前快照 */
    @Column(name = "before_snapshot", columnDefinition = "TEXT")
    private String beforeSnapshot;

    /** 变更后快照 */
    @Column(name = "after_snapshot", columnDefinition = "TEXT")
    private String afterSnapshot;

    /** 变更原因 */
    @Column(name = "change_reason")
    private String changeReason;

    /** 操作人 */
    @Column(name = "operator_id")
    private Long operatorId;

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
