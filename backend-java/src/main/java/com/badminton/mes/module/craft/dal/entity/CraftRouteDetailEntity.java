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
 * 工艺路线明细实体，记录顺序、工位、设备、SOP 和质检控制要求。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
@Entity
@DynamicInsert
@DynamicUpdate
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

    /** 默认工位主键 */
    @Column(name = "station_id")
    private Long stationId;

    /** 设备类别要求 */
    @Column(name = "equipment_category_id")
    private Long equipmentCategoryId;

    /** 是否质检节点 */
    @Column(name = "is_inspect")
    private Boolean inspect;

    /** 工序 SOP 关联主键 */
    @Column(name = "sop_id")
    private Long sopId;

    /** 检验方案主键 */
    @Column(name = "quality_plan_id")
    private Long qualityPlanId;

    /** 工序顺序号 */
    @Column(name = "sequence_no")
    private Integer sequenceNo;

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
