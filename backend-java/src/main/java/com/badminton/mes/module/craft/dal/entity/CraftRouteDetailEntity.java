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
 * 工艺路线明细基础实体。
 *
 * <p>当前用于工序删除引用校验；路线模块后续可通过迁移增加工位、设备、SOP 等字段。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
@Entity
@DynamicInsert
@Table(name = "craft_route_detail")
public class CraftRouteDetailEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 工艺路线主键 */
    @Column(name = "route_id")
    private Long routeId;

    /** 工序主键 */
    @Column(name = "process_id")
    private Long processId;

    /** 工序顺序号 */
    @Column(name = "sequence_no")
    private Integer sequenceNo;

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
